package rosa.archive.core.store;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import rosa.archive.core.AbstractFileSystemTest;
import rosa.archive.core.ArchiveCoreModule;
import rosa.archive.core.GuiceJUnitRunner;
import rosa.archive.core.GuiceJUnitRunner.GuiceModules;
import rosa.archive.core.check.BookCollectionChecker;
import rosa.archive.core.config.AppConfig;
import rosa.archive.core.serialize.Serializer;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
@RunWith(GuiceJUnitRunner.class)
@GuiceModules({ArchiveCoreModule.class})
public class StoreIntegrationTest extends AbstractFileSystemTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

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
    public void dontCheckBitsTest() throws Exception {
        Store store = storeFactory.create(base);
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
        Store store = storeFactory.create(base);
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
        Store store = storeFactory.create(base);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        BookCollection collection = store.loadBookCollection("rosedata", errors);
        assertNotNull(collection);

        Book book = store.loadBook("rosedata", "Walters143", errors);
        assertNotNull(book);

        errors.clear();

        boolean check = store.check(collection, book, true, errors, warnings);
        assertFalse(check);
        assertEquals(1169, errors.size());
        assertTrue(errors.contains("Croping information for item [Walters143.138v.tif] missing from parent Book archive. [Walters143]"));
        assertEquals(7, warnings.size());
        assertTrue(warnings.contains("Illustration character ID is non-numeric. Illustration [43], character ID [Galatea]"));
    }
}
