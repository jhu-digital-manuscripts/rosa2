package rosa.archive.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
 * Tests for image list generation, crop, and crop list methods in FileSystemArchiveStore.
 */
class FileSystemArchiveStoreImageTest {

    @Test
    void generateAndWriteImageList_scansImagesAndWritesCsv(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol").resolve("testbook");
        Files.createDirectories(bookDir);

        // Create test images
        writeTestImage(bookDir.resolve("page001.tif"), 100, 200);
        writeTestImage(bookDir.resolve("page002.jpg"), 150, 300);
        // Non-image files should be excluded
        Files.writeString(bookDir.resolve("metadata.xml"), "<metadata/>");

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        store.generateAndWriteImageList("testcol", "testbook", true, errors);

        assertTrue(errors.isEmpty(), "Expected no errors but got: " + errors);

        Path imagesCsv = bookDir.resolve("testbook.images.csv");
        assertTrue(Files.exists(imagesCsv));

        List<String> lines = Files.readAllLines(imagesCsv, StandardCharsets.UTF_8);
        assertEquals(2, lines.size());
        // Sorted alphabetically
        assertTrue(lines.get(0).startsWith("page001.tif,100,200,false"));
        assertTrue(lines.get(1).startsWith("page002.jpg,150,300,false"));
    }

    @Test
    void generateAndWriteImageList_respectsForceFlag(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol").resolve("testbook");
        Files.createDirectories(bookDir);

        writeTestImage(bookDir.resolve("page001.tif"), 100, 200);

        // Pre-existing image list
        Path imagesCsv = bookDir.resolve("testbook.images.csv");
        Files.writeString(imagesCsv, "existing content");

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        // Without force, should not overwrite
        store.generateAndWriteImageList("testcol", "testbook", false, errors);
        assertEquals("existing content", Files.readString(imagesCsv));

        // With force, should overwrite
        store.generateAndWriteImageList("testcol", "testbook", true, errors);
        String content = Files.readString(imagesCsv);
        assertNotEquals("existing content", content);
        assertTrue(content.contains("page001.tif"));
    }

    @Test
    void generateAndWriteImageList_throwsOnMissingBook(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path collectionDir = archiveDir.resolve("testcol");
        Files.createDirectories(collectionDir);

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        assertThrows(IOException.class, () ->
                store.generateAndWriteImageList("testcol", "nonexistent", true, errors));
    }

    @Test
    void generateAndWriteImageList_emptyBookDirectory(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol").resolve("testbook");
        Files.createDirectories(bookDir);

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        store.generateAndWriteImageList("testcol", "testbook", true, errors);

        assertTrue(errors.isEmpty());
        Path imagesCsv = bookDir.resolve("testbook.images.csv");
        assertTrue(Files.exists(imagesCsv));
        assertEquals("", Files.readString(imagesCsv));
    }

    @Test
    void generateAndWriteImageList_sortedOutput(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol").resolve("testbook");
        Files.createDirectories(bookDir);

        // Create images in non-alphabetical order
        writeTestImage(bookDir.resolve("z_page.tif"), 10, 20);
        writeTestImage(bookDir.resolve("a_page.jpg"), 30, 40);
        writeTestImage(bookDir.resolve("m_page.tif"), 50, 60);

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        store.generateAndWriteImageList("testcol", "testbook", true, errors);

        assertTrue(errors.isEmpty());
        List<String> lines = Files.readAllLines(bookDir.resolve("testbook.images.csv"));
        assertEquals(3, lines.size());
        assertTrue(lines.get(0).startsWith("a_page.jpg"));
        assertTrue(lines.get(1).startsWith("m_page.tif"));
        assertTrue(lines.get(2).startsWith("z_page.tif"));
    }

    @Test
    void cropImages_cropsBasedOnCropFile(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol").resolve("testbook");
        Files.createDirectories(bookDir);

        // Create a test image 200x400
        writeTestImage(bookDir.resolve("page001.tif"), 200, 400);

        // Create crop file: crop to region (10, 20, 100, 200) → 90x180
        Files.writeString(bookDir.resolve("testbook.crop.txt"),
                "page001.tif,10,20,100,200\n");

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        store.cropImages("testcol", "testbook", true, errors);

        assertTrue(errors.isEmpty(), "Expected no errors but got: " + errors);

        Path croppedImage = bookDir.resolve("cropped").resolve("page001.tif");
        assertTrue(Files.exists(croppedImage));

        BufferedImage cropped = ImageIO.read(croppedImage.toFile());
        assertNotNull(cropped);
        assertEquals(90, cropped.getWidth());
        assertEquals(180, cropped.getHeight());
    }

    @Test
    void cropImages_respectsForceFlag(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol").resolve("testbook");
        Path croppedDir = bookDir.resolve("cropped");
        Files.createDirectories(croppedDir);

        writeTestImage(bookDir.resolve("page001.tif"), 200, 400);
        Files.writeString(bookDir.resolve("testbook.crop.txt"), "page001.tif,10,20,100,200\n");

        // Pre-existing cropped file (tiny 1x1 image)
        writeTestImage(croppedDir.resolve("page001.tif"), 1, 1);

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        // Without force, should skip
        store.cropImages("testcol", "testbook", false, errors);
        BufferedImage img = ImageIO.read(croppedDir.resolve("page001.tif").toFile());
        assertEquals(1, img.getWidth());

        // With force, should overwrite
        store.cropImages("testcol", "testbook", true, errors);
        img = ImageIO.read(croppedDir.resolve("page001.tif").toFile());
        assertEquals(90, img.getWidth());
    }

    @Test
    void cropImages_missingCropFile(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol").resolve("testbook");
        Files.createDirectories(bookDir);

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        store.cropImages("testcol", "testbook", true, errors);

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Crop file not found"));
    }

    @Test
    void cropImages_invalidCropRegion(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol").resolve("testbook");
        Files.createDirectories(bookDir);

        writeTestImage(bookDir.resolve("page001.tif"), 100, 100);
        // Crop region exceeds image bounds
        Files.writeString(bookDir.resolve("testbook.crop.txt"), "page001.tif,0,0,200,200\n");

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        store.cropImages("testcol", "testbook", true, errors);

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Invalid crop region"));
    }

    @Test
    void cropImages_missingSourceImage(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol").resolve("testbook");
        Files.createDirectories(bookDir);

        Files.writeString(bookDir.resolve("testbook.crop.txt"), "missing.tif,0,0,50,50\n");

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        store.cropImages("testcol", "testbook", true, errors);

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Source image not found"));
    }

    @Test
    void generateAndWriteCropList_scanscroppedDirAndWritesCsv(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol").resolve("testbook");
        Path croppedDir = bookDir.resolve("cropped");
        Files.createDirectories(croppedDir);

        writeTestImage(croppedDir.resolve("page001.tif"), 90, 180);
        writeTestImage(croppedDir.resolve("page002.jpg"), 120, 240);

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        store.generateAndWriteCropList("testcol", "testbook", true, errors);

        assertTrue(errors.isEmpty(), "Expected no errors but got: " + errors);

        Path cropCsv = bookDir.resolve("testbook.images.crop.csv");
        assertTrue(Files.exists(cropCsv));

        List<String> lines = Files.readAllLines(cropCsv, StandardCharsets.UTF_8);
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).startsWith("page001.tif,90,180,false"));
        assertTrue(lines.get(1).startsWith("page002.jpg,120,240,false"));
    }

    @Test
    void generateAndWriteCropList_respectsForceFlag(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol").resolve("testbook");
        Path croppedDir = bookDir.resolve("cropped");
        Files.createDirectories(croppedDir);

        writeTestImage(croppedDir.resolve("page001.tif"), 90, 180);

        Path cropCsv = bookDir.resolve("testbook.images.crop.csv");
        Files.writeString(cropCsv, "old content");

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        // Without force, should not overwrite
        store.generateAndWriteCropList("testcol", "testbook", false, errors);
        assertEquals("old content", Files.readString(cropCsv));

        // With force, should overwrite
        store.generateAndWriteCropList("testcol", "testbook", true, errors);
        String content = Files.readString(cropCsv);
        assertNotEquals("old content", content);
        assertTrue(content.contains("page001.tif"));
    }

    @Test
    void generateAndWriteCropList_missingCroppedDir(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol").resolve("testbook");
        Files.createDirectories(bookDir);

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        store.generateAndWriteCropList("testcol", "testbook", true, errors);

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Cropped directory not found"));
    }

    @Test
    void cropImages_throwsOnMissingBook(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path collectionDir = archiveDir.resolve("testcol");
        Files.createDirectories(collectionDir);

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        assertThrows(IOException.class, () ->
                store.cropImages("testcol", "nonexistent", true, errors));
    }

    @Test
    void generateAndWriteCropList_throwsOnMissingBook(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path collectionDir = archiveDir.resolve("testcol");
        Files.createDirectories(collectionDir);

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        assertThrows(IOException.class, () ->
                store.generateAndWriteCropList("testcol", "nonexistent", true, errors));
    }

    // ---- Helper methods ----

    /**
     * Writes a simple test image file (PNG format stored as .tif or .jpg) at the given path.
     */
    private void writeTestImage(Path path, int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        String filename = path.getFileName().toString().toLowerCase();
        String format;
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            format = "jpg";
        } else {
            // For .tif files, write as PNG since TIFF writer may not be available
            format = "png";
        }
        ImageIO.write(img, format, path.toFile());
    }
}
