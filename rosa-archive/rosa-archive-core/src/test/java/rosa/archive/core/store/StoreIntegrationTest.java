package rosa.archive.core.store;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import rosa.archive.core.AbstractFileSystemTest;
import rosa.archive.core.ArchiveCoreModule;
import rosa.archive.core.GuiceJUnitRunner;
import rosa.archive.core.GuiceJUnitRunner.GuiceModules;
import rosa.archive.core.check.BookCollectionChecker;
import rosa.archive.core.config.AppConfig;
import rosa.archive.core.serialize.Serializer;
import rosa.archive.core.store.Store;
import rosa.archive.core.store.StoreFactory;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.aor.AnnotatedPage;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
@RunWith(GuiceJUnitRunner.class)
@GuiceModules({ArchiveCoreModule.class})
public class StoreIntegrationTest extends AbstractFileSystemTest {

    @Inject
    private AppConfig config;
    @Inject
    private Map<Class, Serializer> serializerMap;
    @Inject
    private StoreFactory storeFactory;

    private BookCollectionChecker collectionChecker;

    @Before
    public void setup() throws URISyntaxException, IOException {
        super.setup();
        collectionChecker = new BookCollectionChecker(config, serializerMap);
    }

    @Test
    @Ignore
    public void dontCheckBitsTest() throws Exception {
        Store store = storeFactory.create(base);
        List<String> errors = new ArrayList<>();

        BookCollection collection = store.loadBookCollection("rosedata", errors);
        assertNotNull(collection);

        boolean check = collectionChecker.checkContent(collection, base.getByteStreamGroup("rosedata"), false, errors);
        assertTrue(check);
    }

    @Test
    @Ignore
    public void doCheckBitsTest() throws Exception {
        Store store = storeFactory.create(base);
        List<String> errors = new ArrayList<>();

        BookCollection collection = store.loadBookCollection("rosedata", errors);
        assertNotNull(collection);

        boolean check = collectionChecker.checkContent(collection, base.getByteStreamGroup("rosedata"), true, errors);
        assertTrue(check);
    }

    @Test
    @Ignore
    public void checkBitsOnBook() throws Exception {
        Store store = storeFactory.create(base);
        List<String> errors = new ArrayList<>();

        BookCollection collection = store.loadBookCollection("rosedata", errors);
        assertNotNull(collection);

        Book book = store.loadBook("rosedata", "Walters143", errors);
        assertNotNull(book);

//        assertEquals(0, errors.size());
        errors.clear();

        boolean check = store.check(collection, book, true, errors);
        assertFalse(check);

        System.out.println("Number of errors: " + errors.size() + "\n");
        for (String error : errors) {
            System.err.println(error);
        }
    }

    @Test
//    @Ignore
    public void readBookWithAnnotations() throws Exception {
        Store store = storeFactory.create(base);
        List<String> errors = new ArrayList<>();

        BookCollection collection = store.loadBookCollection("data", errors);
        assertNotNull(collection);

        Book book = store.loadBook("data", "Domenichi", errors);

        assertNotNull(book);
        assertEquals("Domenichi", book.getId());
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
        assertEquals(2, annotatedPages.size());

        errors.clear();

        boolean check = store.check(collection, book, true, errors);
        assertFalse(check);

        System.out.println("Number of errors: " + errors.size() + "\n");
        for (String error : errors) {
            System.err.println(error);
        }
    }

}
