package rosa.archive.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import rosa.archive.core.util.HashUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that combine multiple ArchiveStore write operations.
 */
class FileSystemArchiveStoreIntegrationTest {

    @Test
    void generateFileMap_thenRenameImages_producesCorrectlyNamedFiles(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol").resolve("TestBook");
        Files.createDirectories(bookDir);

        // Create 6 .tif image files with distinct content
        for (int i = 1; i <= 6; i++) {
            writeTestImage(bookDir.resolve(String.format("orig%03d.tif", i)), 100, 200);
        }

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        // Generate a file map: front cover, 1 frontmatter, 0 endmatter, 0 misc, no back cover
        // total 6: 2 (front cover+pastedown) + 1 (frontmatter flyleaf) + 3 (body)
        store.generateFileMap("testcol", "TestBook", "NewBook", true, false, 1, 0, 0, errors);
        assertTrue(errors.isEmpty(), "generateFileMap errors: " + errors);

        // Verify filemap.csv was created
        Path fileMap = bookDir.resolve("filemap.csv");
        assertTrue(Files.exists(fileMap));

        // Rename images forward
        store.renameImages("testcol", "TestBook", false, false, errors);
        assertTrue(errors.isEmpty(), "renameImages forward errors: " + errors);

        // Verify original files are gone
        for (int i = 1; i <= 6; i++) {
            assertFalse(Files.exists(bookDir.resolve(String.format("orig%03d.tif", i))),
                    "Original file should be renamed");
        }

        // Verify new names exist following archive conventions
        assertTrue(Files.exists(bookDir.resolve("NewBook.binding.frontcover.tif")));
        assertTrue(Files.exists(bookDir.resolve("NewBook.frontmatter.pastedown.tif")));
        assertTrue(Files.exists(bookDir.resolve("NewBook.frontmatter.flyleaf.001r.tif")));

        // Body pages should exist
        long bodyPages = Files.list(bookDir)
                .filter(p -> {
                    String name = p.getFileName().toString();
                    return name.startsWith("NewBook.") && name.matches("NewBook\\.\\d{3}[rv]\\.tif");
                })
                .count();
        assertTrue(bodyPages > 0, "Should have body page files");
    }

    @Test
    void renameImages_forwardThenReverse_restoresOriginalNames(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol").resolve("TestBook");
        Files.createDirectories(bookDir);

        // Create image files with unique content
        Files.writeString(bookDir.resolve("img_a.tif"), "contentA");
        Files.writeString(bookDir.resolve("img_b.tif"), "contentB");
        Files.writeString(bookDir.resolve("img_c.tif"), "contentC");

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        // Generate file map (no covers, no flyleaves, no misc — all body)
        store.generateFileMap("testcol", "TestBook", "New", false, false, 0, 0, 0, errors);
        assertTrue(errors.isEmpty(), "generateFileMap errors: " + errors);

        // Forward rename
        store.renameImages("testcol", "TestBook", false, false, errors);
        assertTrue(errors.isEmpty(), "Forward rename errors: " + errors);

        // Originals should be gone
        assertFalse(Files.exists(bookDir.resolve("img_a.tif")));
        assertFalse(Files.exists(bookDir.resolve("img_b.tif")));
        assertFalse(Files.exists(bookDir.resolve("img_c.tif")));

        // Reverse rename
        store.renameImages("testcol", "TestBook", false, true, errors);
        assertTrue(errors.isEmpty(), "Reverse rename errors: " + errors);

        // Originals should be restored with correct content
        assertTrue(Files.exists(bookDir.resolve("img_a.tif")));
        assertTrue(Files.exists(bookDir.resolve("img_b.tif")));
        assertTrue(Files.exists(bookDir.resolve("img_c.tif")));
        assertEquals("contentA", Files.readString(bookDir.resolve("img_a.tif")));
        assertEquals("contentB", Files.readString(bookDir.resolve("img_b.tif")));
        assertEquals("contentC", Files.readString(bookDir.resolve("img_c.tif")));
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
