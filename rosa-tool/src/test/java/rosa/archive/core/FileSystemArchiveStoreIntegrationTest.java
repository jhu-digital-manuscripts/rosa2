package rosa.archive.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import rosa.archive.core.util.HashUtil;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookDescription;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that combine multiple ArchiveStore write operations.
 */
class FileSystemArchiveStoreIntegrationTest {

    private static final Path TEST_ARCHIVE = Path.of("src/test/resources/archive");

    @Test
    void loadBook_includesEnglishDescription() throws IOException {
        var store = new FileSystemArchiveStore(TEST_ARCHIVE);
        
        // Load the rose collection
        BookCollection collection = store.loadCollection("rose");
        assertNotNull(collection);
        
        // Load Douce195 which has description_en.xml
        Book book = store.loadBook(collection, "Douce195");
        assertNotNull(book);
        
        // Check that English description was loaded
        BookDescription enDesc = book.getDescription("en");
        assertNotNull(enDesc, "English description should be loaded from Douce195.description_en.xml");
        
        // Verify content
        String identification = enDesc.getNote("IDENTIFICATION");
        assertNotNull(identification);
        assertTrue(identification.contains("MS Douce 195"));
        assertTrue(identification.contains("Bodleian Library"));
        
        // Verify full text works
        String fullText = enDesc.getFullText();
        assertFalse(fullText.isBlank());
        assertTrue(fullText.contains("Roman de la Rose"));
    }

    @Test
    void updateChecksum_afterFileModification_detectsChange(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol").resolve("testbook");
        Files.createDirectories(bookDir);

        // Create initial file
        Path file1 = bookDir.resolve("data.xml");
        Files.writeString(file1, "original content");

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        // Compute initial checksum
        store.updateChecksum("testcol", "testbook", true, errors);
        assertTrue(errors.isEmpty());

        Path checksumFile = bookDir.resolve("testbook.SHA1SUM");
        assertTrue(Files.exists(checksumFile));

        String initialChecksum = Files.readString(checksumFile);
        String originalHash = HashUtil.computeSHA1(file1);
        assertTrue(initialChecksum.contains(originalHash));

        // Modify the file
        Files.writeString(file1, "modified content");
        String newHash = HashUtil.computeSHA1(file1);
        assertNotEquals(originalHash, newHash, "File content changed, hash should differ");

        // Force recompute
        store.updateChecksum("testcol", "testbook", true, errors);
        assertTrue(errors.isEmpty());

        String updatedChecksum = Files.readString(checksumFile);
        assertTrue(updatedChecksum.contains(newHash), "Checksum file should contain new hash");
        assertFalse(updatedChecksum.contains(originalHash), "Old hash should be gone");
    }

    @Test
    void generateImageList_afterAddingImages_reflectsNewFiles(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol").resolve("testbook");
        Files.createDirectories(bookDir);

        // Start with one image
        writeTestImage(bookDir.resolve("page001.tif"), 100, 200);

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        // Generate initial image list
        store.generateAndWriteImageList("testcol", "testbook", true, errors);
        assertTrue(errors.isEmpty());

        Path imagesCsv = bookDir.resolve("testbook.images.csv");
        List<String> initialLines = Files.readAllLines(imagesCsv);
        assertEquals(1, initialLines.size());
        assertTrue(initialLines.get(0).contains("page001.tif"));

        // Add another image
        writeTestImage(bookDir.resolve("page002.tif"), 300, 400);

        // Regenerate with force
        store.generateAndWriteImageList("testcol", "testbook", true, errors);
        assertTrue(errors.isEmpty());

        List<String> updatedLines = Files.readAllLines(imagesCsv);
        assertEquals(2, updatedLines.size());
        assertTrue(updatedLines.stream().anyMatch(l -> l.contains("page001.tif")));
        assertTrue(updatedLines.stream().anyMatch(l -> l.contains("page002.tif")));
    }

    // ---- Helper methods ----

    /**
     * Writes a simple test image file (PNG format stored as .tif) at the given path.
     */
    private void writeTestImage(Path path, int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(img, "png", path.toFile());
    }
}
