package rosa.archive.core.store;

import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import rosa.archive.core.AbstractFileSystemTest;
import rosa.archive.core.ArchiveCoreModule;
import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.FSByteStreamGroup;
import rosa.archive.core.GuiceJUnitRunner;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.SHA1Checksum;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
@RunWith(GuiceJUnitRunner.class)
@GuiceJUnitRunner.GuiceModules({ArchiveCoreModule.class})
public class StoreUpdateChecksumIntegrationTest extends AbstractFileSystemTest {
    private static final String COLLECTION = "collection";

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Inject
    private StoreFactory storeFactory;
    private Store store;

    private Path originalPath;
    private Path collectionPath;
    private Path bookPath;

    @Before
    public void setup() throws URISyntaxException, IOException {
        super.setup();
        URL url = getClass().getClassLoader().getResource("data/LudwigXV7");
        assertNotNull(url);
        originalPath = Paths.get(url.toURI());
        assertNotNull(originalPath);

        File folder = tempFolder.newFolder("1");
        collectionPath = Files.createDirectories(folder.toPath().resolve(COLLECTION));
        bookPath = Files.createDirectories(collectionPath.resolve("LudwigXV7"));

        ByteStreamGroup tempGroup = new FSByteStreamGroup(folder.toString());
        assertNotNull(tempGroup);
        assertEquals(0, tempGroup.numberOfByteStreams());
        assertEquals(1, tempGroup.numberOfByteStreamGroups());

        store = storeFactory.create(tempGroup);
        assertNotNull(store);
        assertEquals(1, store.listBookCollections().length);
        assertEquals(1, store.listBooks(COLLECTION).length);
    }

    /**
     * Copy all files from the original path (data/LudwigXV7) to a new book in the temporary folder.
     *
     * @throws IOException
     */
    private void copyTestFiles(Path origin) throws IOException {
        // Copy test files to tmp directory
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(origin, new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(Path entry) throws IOException {
                return Files.isRegularFile(entry);
            }
        })) {
            for (Path path : ds) {
                try (InputStream in = Files.newInputStream(path)) {
                    String name = path.getFileName().toString();
                    Path filePath = bookPath.resolve(name);

                    Files.copy(in, filePath);
                    assertTrue(Files.exists(filePath));
                    assertTrue(Files.isRegularFile(filePath));

                    // For all files except checksum file, touch them to change the last modified time
//                    FileTime time = FileTime.from(System.currentTimeMillis() + 100, TimeUnit.MILLISECONDS);
//                    System.out.println(filePath.toFile().lastModified()
//                            + " :: " + time.toMillis()
//                            + " :: " + filePath.getFileName().toString());
//                    if (!filePath.getFileName().toString().contains("SHA1SUM")) {
//                        Files.setLastModifiedTime(filePath, time);
//                        assertTrue(filePath.toFile().setLastModified(System.currentTimeMillis()));
//                        System.out.println(filePath.toFile().lastModified()
//                                + " :: " + time.toMillis()
//                                + " :: " + filePath.getFileName().toString());
//                    }
                }
            }

            assertEquals(1, collectionPath.toFile().list().length);
            assertEquals(53, bookPath.toFile().list().length);
        }
    }

    private void p() {
        System.out.println();
    }

    private void p(List<String> strings) {
        p(strings.toArray());
    }

    private void p(Object ... objects) {
        for (Object obj : objects) {
            p(obj);
        }
    }

    private void p(Object obj) {
        System.out.println(obj.toString());
    }

    @Test
    public void createChecksumFileForCollection() throws Exception {
        File folder = tempFolder.newFolder("2");
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

        // Create the store for this collection
        ByteStreamGroup base = new FSByteStreamGroup(folder.toPath().toString());
        assertNotNull(base);
        Store store = storeFactory.create(base);
        assertNotNull(store);

        // Load collection
        List<String> errors = new ArrayList<>();
        BookCollection collection = store.loadBookCollection("collection", errors);

        assertNotNull(collection);
        // 1 error comes from serializing the Narrative Sections!!
        assertEquals(1, errors.size());
        errors.clear();

        // Force update of the checksum
        store.updateChecksum(collection, true, errors);
        assertEquals(0, errors.size());
        assertTrue(base.hasByteStreamGroup("collection"));

        // Check the collection
        ByteStreamGroup colGroup = base.getByteStreamGroup("collection");
        assertNotNull(colGroup);
        assertEquals(0, colGroup.numberOfByteStreamGroups());
        assertEquals(4, colGroup.numberOfByteStreams());
        assertTrue(colGroup.hasByteStream("collection.SHA1SUM"));

        // Make sure SHA1SUM exists
        Path shaPath = folder.toPath().resolve("collection/collection.SHA1SUM");
        assertNotNull(shaPath);
        assertTrue(Files.exists(shaPath));

        // Read SHA1SUM
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
    public void createNewChecksums() throws Exception {
        final String BOOK = "LudwigXV7";
        List<String> errors = new ArrayList<>();

        // Copy all files, then delete the SHA1SUM file
        copyTestFiles(originalPath);
        Files.deleteIfExists(bookPath.resolve("LudwigXV7.SHA1SUM"));

        assertEquals(1, store.listBookCollections().length);
        assertEquals(1, store.listBooks("collection").length);
        assertEquals("LudwigXV7", store.listBooks("collection")[0]);

        // Ensure that no SHA1SUM file exists
        ByteStreamGroup bookStreams = new FSByteStreamGroup(bookPath.toString());
        assertNotNull(bookStreams);
        assertEquals(0, bookStreams.numberOfByteStreamGroups());
        assertEquals(52, bookStreams.numberOfByteStreams());
        assertFalse(bookStreams.hasByteStream("LudwigXV7.SHA1SUM"));

        // Load collection + book
        BookCollection collection = store.loadBookCollection(COLLECTION, errors);
        Book book = store.loadBook(COLLECTION, BOOK, errors);

        assertNotNull(collection);
        assertNotNull(book);
        assertEquals(BOOK, book.getId());
        assertNull(book.getSHA1Checksum());
        errors.clear();

        // Update checksum
        assertTrue(store.updateChecksum(collection, book, false, errors));
        assertEquals(0, errors.size());

        // Reload the book to grab new SHA1SUM and validate
        book = store.loadBook(COLLECTION, BOOK, errors);

        SHA1Checksum checksum = book.getSHA1Checksum();
        assertNotNull(checksum);
        assertEquals(52, checksum.getAllIds().size());

        assertEquals(0, badChecksums(collection, book));

        // Read in the file again.
        List<String> newLines = Files.readAllLines(bookPath.resolve("LudwigXV7.SHA1SUM"), Charset.forName("UTF-8"));
        assertNotNull(newLines);

        // Make sure the two lists are different!
        assertEquals(52, newLines.size());

        for (String str : newLines) {
            String[] parts = str.split("\\s+");

            // proper format
            assertNotNull(parts);
            assertEquals(2, parts.length);
            assertTrue(StringUtils.isAlphanumeric(parts[0]));
            assertTrue(parts[1].startsWith("LudwigXV7."));
            assertFalse(parts[1].equals("LudwigXV7.SHA1SUM"));
        }
    }

    @Test
    public void overwriteOldChecksums() throws Exception {
        List<String> errors = new ArrayList<>();

        copyTestFiles(originalPath);

        // Save original data lines
        List<String> originalLines = Files.readAllLines(bookPath.resolve("LudwigXV7.SHA1SUM"), Charset.forName("UTF-8"));
        assertNotNull(originalLines);
        assertEquals(51, originalLines.size());

        // Load collection + book from the store
        BookCollection collection = store.loadBookCollection(COLLECTION, errors);
        Book book = store.loadBook(COLLECTION, "LudwigXV7", errors);

        assertNotNull(book);
        assertNotNull(book.getId());
        assertNotNull(book.getSHA1Checksum());

        // Test data should have 31 bad checksum values
        assertEquals(31, badChecksums(collection, book));

        // Update the checksums.
        errors.clear();
        assertTrue(store.updateChecksum(collection, book, false, errors));
        assertEquals(0, errors.size());

        // Reload the book to grab new SHA1SUM and validate
        assertEquals(0, badChecksums(collection, store.loadBook(COLLECTION, "LudwigXV7", errors)));

        // Read in the file again.
        List<String> newLines = Files.readAllLines(bookPath.resolve("LudwigXV7.SHA1SUM"), Charset.forName("UTF-8"));
        assertNotNull(newLines);

        // Make sure the two lists are different!
        assertNotEquals(originalLines, newLines);
        assertEquals(52, newLines.size());

        for (String str : newLines) {
            String[] parts = str.split("\\s+");

            // proper format
            assertNotNull(parts);
            assertEquals(2, parts.length);
            assertTrue(StringUtils.isAlphanumeric(parts[0]));
            assertTrue(parts[1].startsWith("LudwigXV7."));
        }

    }

    private int badChecksums(BookCollection collection, Book book) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Perform bit check on the book
        assertFalse(store.check(collection, book, true, errors, warnings));

        // Count only the checksum errors
        int badChecksums = 0;
        for (String str : errors) {
            if (str.contains("Calculated hash value is different from stored value!")) {
                badChecksums++;
            }
        }

        return badChecksums;
    }

}
