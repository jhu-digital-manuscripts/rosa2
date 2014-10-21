package rosa.archive.core.check;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import rosa.archive.core.AbstractFileSystemTest;
import rosa.archive.core.ArchiveCoreModule;
import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.FSByteStreamGroup;
import rosa.archive.core.GuiceJUnitRunner;
import rosa.archive.core.GuiceJUnitRunner.GuiceModules;
import rosa.archive.core.config.AppConfig;
import rosa.archive.core.serialize.Serializer;
import rosa.archive.core.store.Store;
import rosa.archive.core.store.StoreFactory;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
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
    @Ignore
    public void dontCheckBitsTest() throws Exception {
        Store store = storeFactory.create(base);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<String>();

        BookCollection collection = store.loadBookCollection("rosedata", errors);
        assertNotNull(collection);

        boolean check = collectionChecker.checkContent(collection, base.getByteStreamGroup("rosedata"), false, errors, warnings);
        assertTrue(check);
    }

    @Test
    @Ignore
    public void doCheckBitsTest() throws Exception {
        Store store = storeFactory.create(base);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        BookCollection collection = store.loadBookCollection("rosedata", errors);
        assertNotNull(collection);

        boolean check = collectionChecker.checkContent(collection, base.getByteStreamGroup("rosedata"), true, errors, warnings);
        assertTrue(check);
    }

    @Test
    @Ignore
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

        System.out.println("Number of errors: " + errors.size() + "\n");
        for (String error : errors) {
            System.err.println(error);
        }
    }

    @Test
    @Ignore
    public void createChecksumFileForCollection() throws Exception {
        File folder = tempFolder.newFolder("1");
        Path col = Files.createDirectory(folder.toPath().resolve("collection"));

        try (InputStream in1 = getClass().getClassLoader().getResourceAsStream("rosedata/character_names.csv");
            InputStream in2 = getClass().getClassLoader().getResourceAsStream("rosedata/illustration_titles.csv");
            InputStream in3 = getClass().getClassLoader().getResourceAsStream("rosedata/narrative_sections.csv")) {

            Files.copy(in1, col.resolve("character_names.csv"));
            Files.copy(in2, col.resolve("illustration_titles.csv"));
            Files.copy(in3, col.resolve("narrative_sections.csv"));
        }

        assertEquals(1, folder.list().length);
        assertEquals(3, col.toFile().list().length);

        ByteStreamGroup base = new FSByteStreamGroup(folder.toPath().toString());
        assertNotNull(base);
        Store store = storeFactory.create(base);
        assertNotNull(store);

        List<String> errors = new ArrayList<>();
        BookCollection collection = store.loadBookCollection("collection", errors);

        assertNotNull(collection);
        // 1 error comes from serializing the Narrative Sections!!
        assertEquals(1, errors.size());
        errors.clear();
    }

}
