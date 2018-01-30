package rosa.iiif.presentation.core.transform.impl;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.core.ArchiveNameParser;
import rosa.archive.core.BaseArchiveTest;
import rosa.archive.core.serialize.AORAnnotatedPageSerializer;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Location;
import rosa.archive.model.aor.Marginalia;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
import rosa.iiif.presentation.model.annotation.Annotation;
import sun.misc.IOUtils;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AnnotationTransformerTest extends BaseArchiveTest {

    private AnnotationTransformer transformer;

    @Before
    public void setup() {
        transformer = new AnnotationTransformer(
                new IIIFPresentationRequestFormatter("SCHEME", "HOST", "PREFIX", 80),
                new ArchiveNameParser()
        );
    }

    @Test
    public void transcriptionTest() throws Exception {
        final String TARGET_PAGE_ID = "LudwigXV7.001v.tif";

        BookCollection collection = loadValidCollection();
        Book book = loadValidLudwigXV7();

        BookImage page = new BookImage(TARGET_PAGE_ID, 3, 3, false);
        page.setName("1r");

        List<Annotation> annotations = transformer.roseTranscriptionOnPage(collection, book, page);
        assertNotNull(annotations);
        assertEquals(1, annotations.size());
    }

    /**
     * Test method {@link AnnotationTransformer#illustrationsForPage(BookCollection, Book, BookImage)}
     * Using test collection and Ludwig book on page 1r
     */
    @Test
    public void illustrationsTest() throws Exception {
        final String TARGET_PAGE_ID = "LudwigXV7.001r.tif";

        BookCollection collection = loadValidCollection();
        Book book = loadValidLudwigXV7();

        // Using faked "page 1r" BookImage
        {
            BookImage page = new BookImage(TARGET_PAGE_ID, 3, 3, false);
            page.setName("1r");

            List<Annotation> annotations = transformer.illustrationsForPage(collection, book, page);
            assertNotNull(annotations);
            assertEquals("Unexpected number of annotations found.", 2, annotations.size());
        }

        // Using actual BookImage
        {
            book.getImages().getImages().stream()
                    .filter(image -> image.getId().equals(TARGET_PAGE_ID))
                    .findFirst()
                    .ifPresent(image -> {
                        List<Annotation> annotations = transformer.illustrationsForPage(collection, book, image);

                        assertNotNull(annotations);
                        assertEquals("Unexpected number of annotations found.", 2, annotations.size());
                    });
        }

    }

    @Test
    public void locationToHtmlNoLocationTest() {
        String result = transformer.locationToHtml();
        assertNotNull("Result was NULL.", result);
        assertTrue("Result was not empty.", result.isEmpty());
    }

    @Test
    public void locationToHtmlTest() {

        List<Location[]> testLocations = testLocations();
        List<String> expected = expected();

        for (int i = 0; i < testLocations.size(); i++) {
            String result = transformer.locationToHtml(testLocations.get(i));

            assertNotNull("Result is NULL.", result);
            assertFalse("Result is empty.", result.isEmpty());
            assertEquals("Unexpected result found.", expected.get(i), result);
        }

    }

    /**
     * Make sure entities are not double-escaped. As XML data is read, entities will be expanded, as normal.
     * However, when the data is translated to HTML, we need to ensure the writer is not too aggressive in
     * escaping these entities. 'á' will be escaped to '&amp;aacute;' by an HTML 4 escape utility. Some
     * writers will then try to escape the ampersand again, resulting in '&amp;amp;accute;'
     *
     * (For the sake of reading in Java: 'á' will be escaped to '&aacute;' by an HTML 4 escape utility. Some
     * writers will then try to escape the ampersand again, resulting in '&amp;accute;')
     *
     * @throws Exception .
     */
    @Test
    public void xmlEntitiesTest() throws Exception {
        String data = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                "<!DOCTYPE transcription SYSTEM \"http://www.livesandletters.ac.uk/schema/aor_20141023.dtd\">\n" +
                "<transcription xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.livesandletters.ac.uk/schema/aor2_18112016.xsd\">\n" +
                "    <page filename=\"BLC120b4.016r.tif\" pagination=\"16\" reader=\"John Dee\"/>\n" +
                "    <annotation>\n" +
                "        <marginalia hand=\"Italian\" method=\"pen\">\n" +
                "            <language ident=\"EN\">\n" +
                "                <position place=\"tail\" book_orientation=\"0\">\n" +
                "                    <marginalia_text>&Sun; - 1/2</marginalia_text>\n" +        // Entity here :: &Sun;  -->   ☉
                "                    <symbol_in_text name=\"Sun\"/>\n" +                        //   - Defined only in DTD
                "                </position>\n" +
                "            </language>\n" +
                "        </marginalia>\n" +
                "        <marginalia hand=\"Italian\" anchor_text=\"Adum&aacute;\" method=\"pen\">\n" +         // á
                "            <language ident=\"LA\">\n" +
                "                <position place=\"right_margin\" book_orientation=\"0\">\n" +
                "                    <marginalia_text>\n" +
                "                        Nota quod dicat Adum&aacute;, tantu[m], no[n] adiecta particula illa Voarch[adumia]:\n" +
                "                    </marginalia_text>\n" +
                "                    <person name=\"Adum&aacute;\"/>\n" +
                "                    <book title=\"Voarchadumia\"/>\n" +
                "                </position>\n" +
                "            </language>\n" +
                "            <translation>Note what Aduma says, only that particular was not aimed at Voarchadumia:</translation>\n" +
                "        </marginalia>\n" +
                "    </annotation>\n" +
                "</transcription>";

        BookCollection col = new BookCollection();
        col.setId("Moo");

        Book book = new Book();
        book.setId("The Greatest Moo");

        BookImage image = new BookImage("FirstMoo", 3, 3, false);

        AORAnnotatedPageSerializer serializer = new AORAnnotatedPageSerializer();
        List<String> errors = new ArrayList<>();
        ByteArrayInputStream in = new ByteArrayInputStream(data.getBytes("UTF-8"));

        AnnotatedPage p = serializer.read(in, errors);

        assertNotNull(p);
        assertTrue(errors.isEmpty());
        assertEquals(2, p.getMarginalia().size());

        Annotation a = transformer.transform(col, book, image, p.getMarginalia().get(1));
        assertFalse(a.getDefaultSource().getEmbeddedText().contains("&amp;"));
    }

    private List<Location[]> testLocations() {
        List<Location[]> tests = new ArrayList<>();

        tests.add(new Location[] {Location.LEFT_MARGIN});
        tests.add(new Location[] {Location.LEFT_MARGIN, Location.RIGHT_MARGIN});
        tests.add(new Location[] {Location.LEFT_MARGIN, Location.INTEXT});
        tests.add(new Location[] {Location.HEAD, Location.TAIL, Location.INTEXT});

        return tests;
    }

    private List<String> expected() {
        return Arrays.asList(
                "<i class=\"aor-icon side-left \"></i>",
                "<i class=\"aor-icon side-left side-right \"></i>",
                "<i class=\"aor-icon side-left side-within \"><i class=\"inner\"></i></i>",
                "<i class=\"aor-icon side-top side-bottom side-within \"><i class=\"inner\"></i></i>"
        );
    }

}
