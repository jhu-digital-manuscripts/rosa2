package rosa.archive.core.store;

import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
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
import rosa.archive.core.check.BookCollectionChecker;
import rosa.archive.core.config.AppConfig;
import rosa.archive.core.serialize.Serializer;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
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

    private void p() {
        System.out.println();
    }

    private void p(Object obj) {
        System.out.println(obj.toString());
    }

    private void p(List<String> strings) {
        for (String str : strings) {
            System.out.println(str);
        }
    }

    @Test
    public void dontCheckBitsTest() throws Exception {
        Store store = storeFactory.create(base);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<String>();

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

    @Test
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

        store.updateChecksum(collection, true, errors);
        assertEquals(0, errors.size());
        assertTrue(base.hasByteStreamGroup("collection"));

        ByteStreamGroup colGroup = base.getByteStreamGroup("collection");
        assertNotNull(colGroup);
        assertEquals(0, colGroup.numberOfByteStreamGroups());
        assertEquals(4, colGroup.numberOfByteStreams());
        assertTrue(colGroup.hasByteStream("collection.SHA1SUM"));

        Path shaPath = folder.toPath().resolve("collection/collection.SHA1SUM");
        assertNotNull(shaPath);

        List<String> lines = Files.readAllLines(shaPath, Charset.forName("UTF-8"));
        assertNotNull(lines);
        assertEquals(3, lines.size());
        for (String line : lines) {
            String[] parts = line.split("\\s+");
            assertEquals(2, parts.length);

            String id = parts[1];
            assertTrue(id.equals("narrative_sections.csv") || id.equals("character_names.csv")
                    || id.equals("illustration_titles.csv"));
            assertTrue(StringUtils.isAlphanumeric(parts[0]));
        }
    }

    @Test
    public void updatesExistingChecksum() throws Exception {
        URL url = getClass().getClassLoader().getResource("data/Walters143");
        assertNotNull(url);
        Path original = Paths.get(url.toURI());
        assertNotNull(original);

        File folder = tempFolder.newFolder("1");
        Path collectionPath = Files.createDirectories(folder.toPath().resolve("collection"));
        Path bookPath = Files.createDirectories(collectionPath.resolve("book"));

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(original, new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(Path entry) throws IOException {
                return Files.isRegularFile(entry);
            }
        })) {
            for (Path path : ds) {
                try (InputStream in = Files.newInputStream(path)) {
                    String name = "book" + path.getFileName().toString().substring(10);
                    Path filePath = bookPath.resolve(name);

                    Files.copy(in, filePath);
                    assertTrue(Files.exists(filePath));
                    assertTrue(Files.isRegularFile(filePath));
                }
            }

            assertEquals(1, collectionPath.toFile().list().length);
            assertEquals(8, bookPath.toFile().list().length);
        }

        // Save original data lines
        List<String> originalLines = Files.readAllLines(bookPath.resolve("book.SHA1SUM"), Charset.forName("UTF-8"));
        assertNotNull(originalLines);
        assertEquals(13, originalLines.size());

        ByteStreamGroup tempGroup = new FSByteStreamGroup(folder.toString());
        assertNotNull(tempGroup);
        assertEquals(0, tempGroup.numberOfByteStreams());
        assertEquals(1, tempGroup.numberOfByteStreamGroups());

        Store store = storeFactory.create(tempGroup);
        assertNotNull(store);
        assertEquals(1, store.listBookCollections().length);
        assertEquals(1, store.listBooks("collection").length);

        List<String> errors = new ArrayList<>();
        Book book = store.loadBook("collection", "book", errors);

        assertNotNull(book);
        assertNotNull(book.getId());
        assertNotNull(book.getSHA1Checksum());

        errors.clear();
        assertTrue(store.updateChecksum("collection", "book", false, errors));

        List<String> newLines = Files.readAllLines(bookPath.resolve("book.SHA1SUM"), Charset.forName("UTF-8"));
        assertNotNull(newLines);
        assertTrue(errors.isEmpty());

        assertNotEquals(originalLines, newLines);
        assertEquals(7, newLines.size());

        for (String str : newLines) {
            String[] parts = str.split("\\s+");

            // proper format
            assertNotNull(parts);
            assertEquals(2, parts.length);
            assertTrue(StringUtils.isAlphanumeric(parts[0]));
            // Make sure all of the old 'Walters143' data has been cleared out
            // Replaced by new 'book' data
            assertTrue(parts[1].startsWith("book."));
        }

    }

}
