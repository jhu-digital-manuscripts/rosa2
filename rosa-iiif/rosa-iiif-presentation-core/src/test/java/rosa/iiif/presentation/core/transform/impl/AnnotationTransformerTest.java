package rosa.iiif.presentation.core.transform.impl;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.core.ArchiveNameParser;
import rosa.archive.core.BaseArchiveTest;
import rosa.archive.core.serialize.AORAnnotatedPageSerializer;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.CollectionMetadata;
import rosa.archive.model.ImageList;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Location;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.Position;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
import rosa.iiif.presentation.model.annotation.Annotation;

import java.io.IOException;
import java.io.InputStream;
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
    public void setup() throws IOException {
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

    /**
     * Test the annotation transformer to make sure that the list of people in
     * marginalia is output correctly. Only applicable to AOR transcriptions.
     * No marginalia exist in the test data that has people specified, so the data
     * will be faked.
     *
     * @throws Exception .
     */
    @Test
    public void peopleListTest() throws Exception {
       BookCollection col = loadValidCollection();
       Book book = loadValidFolgersHa2();

       // Grab a random marginalia and insert a known list of people
        AnnotatedPage annotatedPage = book.getAnnotatedPages().get(0);
        assertNotNull("Failed to load annotated page.", annotatedPage);

        Marginalia marg = annotatedPage.getMarginalia().get(0);
        Position pos = marg.getLanguages().get(0).getPositions().get(0);
        pos.setPeople(Arrays.asList("Jim", "Mark", "Jeff"));

        Annotation anno = transformer.transform(col, book, marg);

        String text = anno.getDefaultSource().getEmbeddedText();

        assertTrue("List of people failed to appear.", text.contains("People:"));
        assertTrue("List of names not found in marginalia translation.",
                text.substring(text.indexOf("People:")).contains("Jim, Mark, Jeff"));
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
     * For CNI stuff, test annotation decoration. Folgers page 2v should contain
     * several external links.
     *
     * @throws Exception .
     */
    @Test
    public void decorateWithPersuesTest() throws Exception {
        BookCollection col = loadValidCollection();
        Book book  = loadValidFolgersHa2();

        AnnotatedPage ap = book.getAnnotationPage("FolgersHa2.002v.tif");

        // First marginalia talks about Venus. This should be decorated with an appropriate CTS URL
        Annotation result = transformer.transform(col, book, ap.getMarginalia().get(0));
        assertTrue(result.getDefaultSource().getEmbeddedText()
                .contains("http://cts.perseids.org/read/pdlrefwk/viaf88890045/003/perseus-eng1/U.venus_1"));
        assertFalse("Found probable escaped HTML", result.getDefaultSource().getEmbeddedText().contains("&lt;"));
    }

    /**
     * The sample Hamlet page has a few features that the base AOR test data does not. Exercise these aspects
     * in the transformer.
     *
     * - A marginalia defines its own ID
     * - New 'reference' that points from the marginalia (with an ID) to an external URL
     *
     * @throws Exception .
     */
    @Test
    public void hamletAnnotationsTest() throws Exception {
        BookCollection fakeCol = new BookCollection();
        fakeCol.setId("The many moos of Hamlet");
        fakeCol.setMetadata(new CollectionMetadata());
        fakeCol.getMetadata().setLanguages(new String[] {"en"});

        Book b = new Book();
        b.setId("Ghost's moo");
        b.setImages(new ImageList());
        b.getImages().getImages().add(new BookImage("Hamlet.001r.tif", 3, 3, false));

        try (InputStream in = getClass().getClassLoader().getResourceAsStream("data/collection/Hamlet/Hamlet.aor.001r.xml")) {
            List<String> err = new ArrayList<>();

            AORAnnotatedPageSerializer serializer = new AORAnnotatedPageSerializer();
            AnnotatedPage page = serializer.read(in, err);

            assertNotNull(page);
            assertTrue(err.isEmpty());

            // Hamlet annotations serialized, now we can transform them
            checkPageAnnos(page.getMarginalia(), fakeCol, b);
            checkPageAnnos(page.getRefs(), fakeCol, b);
        }
    }

    private void checkPageAnnos(List<? extends rosa.archive.model.aor.Annotation> annos, BookCollection c, Book b) {
        annos.stream().map(a -> transformer.transform(c, b, a))
                .forEach(a -> {
                    assertNotNull(a);
                    assertNotNull(a.getDefaultSource());
                    assertNotNull(a.getDefaultTarget());
                });
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
