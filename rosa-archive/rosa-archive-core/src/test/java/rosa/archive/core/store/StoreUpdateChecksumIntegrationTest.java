package rosa.archive.core.store;

import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
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
import java.util.Arrays;
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
public class StoreUpdateChecksumIntegrationTest extends StoreIntegrationBase {
    private static final String COLLECTION = "collection";

    @Inject
    private StoreFactory storeFactory;

    @Test
    public void createChecksumFileForCollection() throws Exception {
        File folder = tempFolder.newFolder("2");
        Path col = Files.createDirectory(folder.toPath().resolve("collection"));

        copyTestFiles(defaultPath.getParent(), col);

        assertEquals(1, folder.list().length);
        assertEquals(5, col.toFile().list().length);

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
        assertEquals(6, colGroup.numberOfByteStreams());
        assertTrue(colGroup.hasByteStream("collection.SHA1SUM"));

        // Make sure SHA1SUM exists
        Path shaPath = folder.toPath().resolve("collection/collection.SHA1SUM");
        assertNotNull(shaPath);
        assertTrue(Files.exists(shaPath));

        // Read SHA1SUM
        List<String> lines = Files.readAllLines(shaPath, Charset.forName("UTF-8"));
        assertNotNull(lines);
        assertEquals(5, lines.size());
        for (String line : lines) {
            String[] parts = line.split("\\s+");
            assertEquals(2, parts.length);

            String id = parts[1];
            assertTrue(id.equals("narrative_sections.csv") || id.equals("character_names.csv")
                    || id.equals("illustration_titles.csv") || id.equals("missing.txt")
                    || id.equals("missing_image.tif"));
            assertTrue(StringUtils.isAlphanumeric(parts[0]));
        }
    }

    @Test
    public void createNewChecksums() throws Exception {
        final String BOOK = "LudwigXV7";
        List<String> errors = new ArrayList<>();

        Path collectionPath = Files.createDirectories(folder.toPath().resolve(COLLECTION));
        Path bookPath = Files.createDirectories(collectionPath.resolve(BOOK));

        // Copy all files, then delete the SHA1SUM file
        copyTestFiles(defaultPath, bookPath);
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
        Path collectionPath = Files.createDirectory(folder.toPath().resolve(COLLECTION));
        Path bookPath = Files.createDirectory(collectionPath.resolve("LudwigXV7"));

        copyTestFiles(defaultPath, bookPath);

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
