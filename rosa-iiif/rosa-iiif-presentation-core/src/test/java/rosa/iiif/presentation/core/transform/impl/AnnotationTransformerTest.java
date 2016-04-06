package rosa.iiif.presentation.core.transform.impl;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.core.ArchiveNameParser;
import rosa.archive.core.BaseArchiveTest;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.aor.Location;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
import rosa.iiif.presentation.model.annotation.Annotation;

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

        System.out.println(Arrays.toString("one\ntwo".split("\\n")));
        System.out.println("<l>Quiconques cuide ne qui die<milestone n=\"11\" ed=\"lecoy\" unit=\"line\"/>".startsWith("<l>"));

        List<Annotation> annotations = transformer.roseTranscriptionOnPage(collection, book, page);
        assertNotNull(annotations);
        assertEquals(1, annotations.size());
        System.out.println(annotations.get(0).getDefaultSource().getEmbeddedText());
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
