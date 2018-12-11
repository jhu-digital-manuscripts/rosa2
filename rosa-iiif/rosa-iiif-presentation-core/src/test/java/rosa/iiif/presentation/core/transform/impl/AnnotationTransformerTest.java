package rosa.iiif.presentation.core.transform.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import rosa.archive.core.ArchiveNameParser;
import rosa.archive.core.BaseArchiveTest;
import rosa.archive.core.serialize.AORAnnotatedPageSerializer;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Symbol;
import rosa.iiif.image.core.IIIFRequestFormatter;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
import rosa.iiif.presentation.core.PresentationTestUtils;
import rosa.iiif.presentation.core.PresentationUris;
import rosa.iiif.presentation.core.StaticResourceRequestFormatter;
import rosa.iiif.presentation.model.annotation.Annotation;

public class AnnotationTransformerTest extends BaseArchiveTest {

    private AnnotationTransformer transformer;

    // Test data for stuff not in test archive
    private BookCollection col;
    private Book book;
    private BookImage img;
    private AnnotatedPage loadedPage;

    @Before
    public void setup() throws Exception {
        IIIFPresentationRequestFormatter formatter =
                new IIIFPresentationRequestFormatter("SCHEME", "HOST", "PREFIX", 80);
        IIIFRequestFormatter imageFormatter = new IIIFRequestFormatter("http", "localhost", 8080, "/image");
        StaticResourceRequestFormatter staticFormatter = new StaticResourceRequestFormatter("http", "localhost", "/data", 80);
        PresentationUris pres_uris = new PresentationUris(formatter, imageFormatter, staticFormatter);
        
        transformer = new AnnotationTransformer(pres_uris, new ArchiveNameParser(), PresentationTestUtils.htmlAdapterSet(pres_uris));

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
                "        <symbol language=\"LA\" name=\"SS\" place=\"head\" text=\"Eccè principia istius Artis, in P. Gregorij Arte mirabili de militia: capite, de Excercitu classico.\"/>\n" + 
                "    </annotation>\n" +
                "</transcription>";

        col = new BookCollection();
        col.setId("Moo");

        book = new Book();
        book.setId("The Greatest Moo");

        img = new BookImage("FirstMoo", 3, 3, false);

        AORAnnotatedPageSerializer serializer = new AORAnnotatedPageSerializer();
        List<String> errors = new ArrayList<>();
        ByteArrayInputStream in = new ByteArrayInputStream(data.getBytes("UTF-8"));

        loadedPage = serializer.read(in, errors);
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
        assertNotNull(loadedPage);
        assertEquals(2, loadedPage.getMarginalia().size());

        Annotation a = transformer.transform(col, book, img, loadedPage.getMarginalia().get(1));
        assertFalse(a.getDefaultSource().getEmbeddedText().contains("&amp;"));
    }

    @Test
    public void testLists() throws Exception {
        Annotation a = transformer.transform(col, book, img, loadedPage.getMarginalia().get(1));
        String text = a.getDefaultSource().getEmbeddedText();

        assertNotNull(text);
        assertFalse(text.isEmpty());
        assertFalse("Annotation text should not contain '[]'", text.contains("[]"));
    }

    @Test
    public void testSymbol() {
        assertEquals(1, loadedPage.getSymbols().size());
        
        Symbol s = loadedPage.getSymbols().get(0);
        Annotation a = transformer.transform(col, book, img, s);
        String text = a.getDefaultSource().getEmbeddedText();

        // Symbol should not include referenced text
        System.err.println(text);
        assertFalse(text.contains("mirabili"));
    }
}
