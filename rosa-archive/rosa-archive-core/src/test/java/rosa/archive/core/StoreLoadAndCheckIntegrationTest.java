package rosa.archive.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import rosa.archive.core.check.BookCollectionChecker;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.meta.MultilangMetadata;

/**
 *
 */
// TODO Fix and cleanup
@RunWith(GuiceJUnitRunner.class)
@GuiceJUnitRunner.GuiceModules({ArchiveCoreModule.class})
@Ignore
public class StoreLoadAndCheckIntegrationTest extends BaseGuiceTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private BookCollectionChecker collectionChecker;

    @Before
    public void setup() throws URISyntaxException, IOException {
        collectionChecker = new BookCollectionChecker(serializers);
    }

    @Test
    public void dontCheckBitsTest() throws Exception {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        BookCollection collection = store.loadBookCollection("rosedata", errors);
        assertNotNull(collection);
        errors.clear();

        boolean check = collectionChecker.checkContent(collection, base.getByteStreamGroup("rosedata"), false, errors, warnings);
        assertFalse(check);
        assertEquals(1, errors.size());
        assertEquals("Malformed row in narrative sections [264]: []", errors.get(0));
        assertEquals(0, warnings.size());
    }

    @Test
    public void doCheckBitsTest() throws Exception {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        BookCollection collection = store.loadBookCollection("rosedata", errors);
        errors.clear();
        assertNotNull(collection);

        boolean check = collectionChecker.checkContent(collection, base.getByteStreamGroup("rosedata"), true, errors, warnings);
        assertFalse(check);
        assertEquals(1, errors.size());
        assertEquals("Malformed row in narrative sections [264]: []", errors.get(0));
        assertEquals(0, warnings.size());
    }

    @Test
    public void checkBitsOnBook() throws Exception {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        BookCollection collection = store.loadBookCollection("rosedata", errors);
        assertNotNull(collection);

        Book book = store.loadBook("rosedata", "Walters143", errors);
        assertNotNull(book);

        errors.clear();

        boolean check = store.check(collection, book, true, errors, warnings);
        assertFalse(check);
        assertEquals(1165, errors.size());
        assertTrue(errors.contains("Cropping information for item [Walters143.138v.tif] missing from parent Book archive. [Walters143]"));
        assertEquals(0, warnings.size());
    }

    @Test
    public void loadWithMultilangMetadata() throws Exception {
        List<String> errors = new ArrayList<>();

        BookCollection collection = store.loadBookCollection("rosedata", errors);
        assertNotNull(collection);

        Book book = store.loadBook("rosedata", "Morgan948", errors);
        assertNotNull(book);
        errors.clear();

        MultilangMetadata mm = book.getMultilangMetadata();
        assertNotNull(mm);
    }

    @Test
    public void readBookWithAnnotations() throws Exception {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        BookCollection collection = store.loadBookCollection("data", errors);
        assertNotNull(collection);

        Book book = store.loadBook("data", "Ha2", errors);

        assertNotNull(book);
        assertEquals("Ha2", book.getId());
        assertNull(book.getImages());
        assertNull(book.getCropInfo());
        assertNull(book.getSHA1Checksum());
        assertNotNull(book.getContent());
        assertNull(book.getBookStructure());
        assertNull(book.getIllustrationTagging());
        assertNull(book.getManualNarrativeTagging());
        assertNull(book.getAutomaticNarrativeTagging());
        assertEquals(0, book.getPermissionsInAllLanguages().length);
        assertNull(book.getBookMetadata("en"));
        assertNull(book.getBookDescription("en"));
        assertNull(book.getTranscription());

        List<AnnotatedPage> annotatedPages = book.getAnnotatedPages();
        assertNotNull(annotatedPages);
        assertEquals(9, annotatedPages.size());

        errors.clear();

        boolean check = store.check(collection, book, true, errors, warnings);
        assertFalse(check);
        assertEquals(17, errors.size());
        assertTrue(errors.contains(
                "[Error: Ha2.019v.xml] (9:78): cvc-complex-type.3.2.2: " +
                "Attribute 'blah' is not allowed to appear in element 'page'."
        ));
    }
}
