package rosa.archive.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import rosa.archive.core.util.HashUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the checksum update operations in FileSystemArchiveStore.
 */
class FileSystemArchiveStoreChecksumTest {

    @Test
    void updateChecksumForBook_force_computesAllFiles(@TempDir Path tempDir) throws IOException {
        // Set up archive structure: archive/collection/book/
        Path archiveDir = tempDir.resolve("archive");
        Path collectionDir = archiveDir.resolve("testcol");
        Path bookDir = collectionDir.resolve("testbook");
        Files.createDirectories(bookDir);

        // Create some files in the book directory
        Files.writeString(bookDir.resolve("file1.xml"), "content1");
        Files.writeString(bookDir.resolve("file2.csv"), "content2");

        FileSystemArchiveStore store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        store.updateChecksum("testcol", "testbook", true, errors);

        assertTrue(errors.isEmpty(), "Expected no errors but got: " + errors);

        // Verify checksum file was created
        Path checksumFile = bookDir.resolve("testbook.SHA1SUM");
        assertTrue(Files.exists(checksumFile));

        // Verify content matches expected format: "hash  filename"
        String content = Files.readString(checksumFile);
        String expectedHash1 = HashUtil.computeSHA1(bookDir.resolve("file1.xml"));
        String expectedHash2 = HashUtil.computeSHA1(bookDir.resolve("file2.csv"));

        assertTrue(content.contains(expectedHash1 + "  file1.xml"));
        assertTrue(content.contains(expectedHash2 + "  file2.csv"));
    }

    @Test
    void updateChecksumForBook_force_excludesChecksumFileItself(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path collectionDir = archiveDir.resolve("testcol");
        Path bookDir = collectionDir.resolve("testbook");
        Files.createDirectories(bookDir);

        Files.writeString(bookDir.resolve("file1.xml"), "content1");
        // Pre-existing checksum file
        Files.writeString(bookDir.resolve("testbook.SHA1SUM"), "oldhash  file1.xml\n");

        FileSystemArchiveStore store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        store.updateChecksum("testcol", "testbook", true, errors);

        assertTrue(errors.isEmpty());

        String content = Files.readString(bookDir.resolve("testbook.SHA1SUM"));
        // The checksum file should NOT contain an entry for itself
        assertFalse(content.contains("testbook.SHA1SUM"));
        // But should contain the real file
        assertTrue(content.contains("file1.xml"));
    }

    @Test
    void updateChecksumForBook_nonForce_reusesUnchangedEntries(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path collectionDir = archiveDir.resolve("testcol");
        Path bookDir = collectionDir.resolve("testbook");
        Files.createDirectories(bookDir);

        // Create a file
        Path file1 = bookDir.resolve("file1.xml");
        Files.writeString(file1, "content1");

        // Create checksum file with a known (fake) hash, set its time AFTER the file's time
        Path checksumFile = bookDir.resolve("testbook.SHA1SUM");
        Files.writeString(checksumFile, "fakehash123456789012345678901234abcdef01  file1.xml\n");

        // Make the checksum file newer than the data file
        FileTime future = FileTime.fromMillis(System.currentTimeMillis() + 10000);
        Files.setLastModifiedTime(checksumFile, future);
        FileTime past = FileTime.fromMillis(System.currentTimeMillis() - 10000);
        Files.setLastModifiedTime(file1, past);

        FileSystemArchiveStore store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        store.updateChecksum("testcol", "testbook", false, errors);

        assertTrue(errors.isEmpty());

        // The fake hash should be preserved because the file is older than the checksum file
        String content = Files.readString(checksumFile);
        assertTrue(content.contains("fakehash123456789012345678901234abcdef01  file1.xml"));
    }

    @Test
    void updateChecksumForBook_nonForce_recomputesNewerFiles(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path collectionDir = archiveDir.resolve("testcol");
        Path bookDir = collectionDir.resolve("testbook");
        Files.createDirectories(bookDir);

        // Create a file
        Path file1 = bookDir.resolve("file1.xml");
        Files.writeString(file1, "content1");

        // Create checksum file with an old hash, set its time BEFORE the file's time
        Path checksumFile = bookDir.resolve("testbook.SHA1SUM");
        Files.writeString(checksumFile, "oldhash1234567890123456789012345678abcdef  file1.xml\n");

        // Make the data file newer than the checksum file
        FileTime past = FileTime.fromMillis(System.currentTimeMillis() - 10000);
        Files.setLastModifiedTime(checksumFile, past);
        FileTime future = FileTime.fromMillis(System.currentTimeMillis() + 10000);
        Files.setLastModifiedTime(file1, future);

        FileSystemArchiveStore store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        store.updateChecksum("testcol", "testbook", false, errors);

        assertTrue(errors.isEmpty());

        // The hash should now be recomputed
        String content = Files.readString(checksumFile);
        String expectedHash = HashUtil.computeSHA1(file1);
        assertTrue(content.contains(expectedHash + "  file1.xml"));
        assertFalse(content.contains("oldhash1234567890123456789012345678abcdef"));
    }

    @Test
    void updateChecksumForBook_nonForce_computesMissingFiles(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path collectionDir = archiveDir.resolve("testcol");
        Path bookDir = collectionDir.resolve("testbook");
        Files.createDirectories(bookDir);

        // Create two files
        Files.writeString(bookDir.resolve("file1.xml"), "content1");
        Files.writeString(bookDir.resolve("file2.csv"), "content2");

        // Create checksum file with only one entry
        Path checksumFile = bookDir.resolve("testbook.SHA1SUM");
        String realHash1 = HashUtil.computeSHA1(bookDir.resolve("file1.xml"));
        Files.writeString(checksumFile, realHash1 + "  file1.xml\n");

        // Make checksum file newer than both data files
        FileTime future = FileTime.fromMillis(System.currentTimeMillis() + 10000);
        Files.setLastModifiedTime(checksumFile, future);
        FileTime past = FileTime.fromMillis(System.currentTimeMillis() - 10000);
        Files.setLastModifiedTime(bookDir.resolve("file1.xml"), past);
        Files.setLastModifiedTime(bookDir.resolve("file2.csv"), past);

        FileSystemArchiveStore store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        store.updateChecksum("testcol", "testbook", false, errors);

        assertTrue(errors.isEmpty());

        // file2 should have been computed since it was missing
        String content = Files.readString(checksumFile);
        String expectedHash2 = HashUtil.computeSHA1(bookDir.resolve("file2.csv"));
        assertTrue(content.contains(expectedHash2 + "  file2.csv"));
        // file1 should be unchanged
        assertTrue(content.contains(realHash1 + "  file1.xml"));
    }

    @Test
    void updateChecksumForCollection_computesCollectionLevelFiles(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path collectionDir = archiveDir.resolve("testcol");
        Files.createDirectories(collectionDir);

        // Create collection-level files
        Files.writeString(collectionDir.resolve("character_names.csv"), "name,id\n");
        Files.writeString(collectionDir.resolve("people.csv"), "person data");

        // Also create a book subdirectory (should be ignored by collection-level checksum)
        Path bookDir = collectionDir.resolve("testbook");
        Files.createDirectories(bookDir);
        Files.writeString(bookDir.resolve("somefile.xml"), "book content");

        FileSystemArchiveStore store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        store.updateChecksum("testcol", true, errors);

        assertTrue(errors.isEmpty());

        // Collection-level checksum file
        Path checksumFile = collectionDir.resolve("testcol.SHA1SUM");
        assertTrue(Files.exists(checksumFile));

        String content = Files.readString(checksumFile);
        // Should contain collection-level files
        assertTrue(content.contains("character_names.csv"));
        assertTrue(content.contains("people.csv"));
        // Should NOT contain files from book subdirectories
        assertFalse(content.contains("somefile.xml"));
    }

    @Test
    void updateChecksumForBook_throwsOnMissingBook(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path collectionDir = archiveDir.resolve("testcol");
        Files.createDirectories(collectionDir);

        FileSystemArchiveStore store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        assertThrows(IOException.class, () ->
                store.updateChecksum("testcol", "nonexistent", true, errors));
    }

    @Test
    void updateChecksumForCollection_throwsOnMissingCollection(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Files.createDirectories(archiveDir);

        FileSystemArchiveStore store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        assertThrows(IOException.class, () ->
                store.updateChecksum("nonexistent", true, errors));
    }

    @Test
    void updateChecksumForBook_emptyDirectory(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path collectionDir = archiveDir.resolve("testcol");
        Path bookDir = collectionDir.resolve("testbook");
        Files.createDirectories(bookDir);

        FileSystemArchiveStore store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        store.updateChecksum("testcol", "testbook", true, errors);

        assertTrue(errors.isEmpty());

        // Checksum file should exist but be empty
        Path checksumFile = bookDir.resolve("testbook.SHA1SUM");
        assertTrue(Files.exists(checksumFile));
        assertEquals("", Files.readString(checksumFile));
    }

    @Test
    void updateChecksumForBook_checksumFileFormat(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path collectionDir = archiveDir.resolve("testcol");
        Path bookDir = collectionDir.resolve("testbook");
        Files.createDirectories(bookDir);

        Files.writeString(bookDir.resolve("test.xml"), "hello");

        FileSystemArchiveStore store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        store.updateChecksum("testcol", "testbook", true, errors);

        assertTrue(errors.isEmpty());

        // Verify the format is "hash  filename" with two spaces (sha1sum compatible)
        String content = Files.readString(bookDir.resolve("testbook.SHA1SUM"));
        String[] lines = content.split("\n");
        assertEquals(1, lines.length);
        // Two spaces between hash and filename
        assertTrue(lines[0].matches("[0-9a-f]{40}  test\\.xml"));
    }
}
