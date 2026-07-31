package rosa.archive.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for file map generation, image renaming, and transcription renaming
 * operations in FileSystemArchiveStore.
 */
class FileSystemArchiveStoreFileMapTest {

    @Test
    void generateFileMap_createsCorrectMappings(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol").resolve("TestBook");
        Files.createDirectories(bookDir);

        // Create 10 .tif files (sorted alphabetically)
        for (int i = 1; i <= 10; i++) {
            Files.writeString(bookDir.resolve(String.format("TestBook.%03d.tif", i)), "image");
        }

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        // Front cover + pastedown (2), 2 frontmatter flyleaves, 0 endmatter, 0 misc
        // That leaves 10 - 2 - 2 - 2 - 0 = 4 body pages (with back cover)
        store.generateFileMap("testcol", "TestBook", "NewId", true, true, 2, 2, 0, errors);

        assertTrue(errors.isEmpty(), "Expected no errors but got: " + errors);

        Path fileMapPath = bookDir.resolve("filemap.csv");
        assertTrue(Files.exists(fileMapPath));

        String content = Files.readString(fileMapPath);
        List<String> lines = content.lines().filter(l -> !l.isBlank()).toList();
        assertEquals(10, lines.size(), "Should have 10 mappings for 10 images");

        // Verify naming patterns exist
        assertTrue(content.contains("NewId.binding.frontcover.tif"), "Should have front cover");
        assertTrue(content.contains("NewId.frontmatter.pastedown.tif"), "Should have front pastedown");
        assertTrue(content.contains("NewId.frontmatter.flyleaf.001r.tif"), "Should have first flyleaf recto");
        assertTrue(content.contains("NewId.frontmatter.flyleaf.001v.tif"), "Should have first flyleaf verso");
        assertTrue(content.contains("NewId.001r.tif"), "Should have first body recto");
        assertTrue(content.contains("NewId.endmatter.flyleaf.001r.tif"), "Should have endmatter flyleaf recto");
        assertTrue(content.contains("NewId.endmatter.flyleaf.001v.tif"), "Should have endmatter flyleaf verso");
        assertTrue(content.contains("NewId.endmatter.pastedown.tif"), "Should have back pastedown");
        assertTrue(content.contains("NewId.binding.backcover.tif"), "Should have back cover");
    }

    @Test
    void generateFileMap_noCoverNorMisc(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol").resolve("TestBook");
        Files.createDirectories(bookDir);

        // Create 6 .tif files
        for (int i = 1; i <= 6; i++) {
            Files.writeString(bookDir.resolve(String.format("img%03d.tif", i)), "image");
        }

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        // No front cover, no back cover, 1 frontmatter, 1 endmatter, 0 misc
        // Body pages = 6 - 1 - 1 = 4
        store.generateFileMap("testcol", "TestBook", "Book", false, false, 1, 1, 0, errors);

        assertTrue(errors.isEmpty(), "Expected no errors: " + errors);

        String content = Files.readString(bookDir.resolve("filemap.csv"));
        assertFalse(content.contains("frontcover"), "No front cover expected");
        assertFalse(content.contains("backcover"), "No back cover expected");
        assertTrue(content.contains("Book.frontmatter.flyleaf.001r.tif"));
        assertTrue(content.contains("Book.001r.tif"));
        assertTrue(content.contains("Book.endmatter.flyleaf.001r.tif"));
    }

    @Test
    void generateFileMap_withMiscImages(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol").resolve("TestBook");
        Files.createDirectories(bookDir);

        // Create 5 .tif files
        for (int i = 1; i <= 5; i++) {
            Files.writeString(bookDir.resolve(String.format("file%03d.tif", i)), "image");
        }

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        // No covers, 0 frontmatter, 0 endmatter, 2 misc
        // Body pages = 5 - 2 = 3
        store.generateFileMap("testcol", "TestBook", "BK", false, false, 0, 0, 2, errors);

        assertTrue(errors.isEmpty(), "Expected no errors: " + errors);

        String content = Files.readString(bookDir.resolve("filemap.csv"));
        assertTrue(content.contains("BK.misc.001.tif"), "Should have misc.001");
        assertTrue(content.contains("BK.misc.002.tif"), "Should have misc.002");
    }

    @Test
    void generateFileMap_throwsOnMissingBook(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Files.createDirectories(archiveDir.resolve("testcol"));

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        assertThrows(IOException.class, () ->
                store.generateFileMap("testcol", "nonexistent", "id", false, false, 0, 0, 0, errors));
    }

    @Test
    void renameImages_forwardRename(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol").resolve("TestBook");
        Files.createDirectories(bookDir);

        // Create image files
        Files.writeString(bookDir.resolve("old1.tif"), "image1");
        Files.writeString(bookDir.resolve("old2.tif"), "image2");

        // Write a filemap.csv
        Files.writeString(bookDir.resolve("filemap.csv"),
                "old1.tif,new1.tif\nold2.tif,new2.tif\n");

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        store.renameImages("testcol", "TestBook", false, false, errors);

        assertTrue(errors.isEmpty(), "Expected no errors: " + errors);
        assertTrue(Files.exists(bookDir.resolve("new1.tif")));
        assertTrue(Files.exists(bookDir.resolve("new2.tif")));
        assertFalse(Files.exists(bookDir.resolve("old1.tif")));
        assertFalse(Files.exists(bookDir.resolve("old2.tif")));
    }

    @Test
    void renameImages_reverseRename(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol").resolve("TestBook");
        Files.createDirectories(bookDir);

        // Start with new-named files (simulate post-rename state)
        Files.writeString(bookDir.resolve("new1.tif"), "image1");
        Files.writeString(bookDir.resolve("new2.tif"), "image2");

        // filemap maps old -> new
        Files.writeString(bookDir.resolve("filemap.csv"),
                "old1.tif,new1.tif\nold2.tif,new2.tif\n");

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        store.renameImages("testcol", "TestBook", false, true, errors);

        assertTrue(errors.isEmpty(), "Expected no errors: " + errors);
        assertTrue(Files.exists(bookDir.resolve("old1.tif")));
        assertTrue(Files.exists(bookDir.resolve("old2.tif")));
        assertFalse(Files.exists(bookDir.resolve("new1.tif")));
        assertFalse(Files.exists(bookDir.resolve("new2.tif")));
    }

    @Test
    void renameImages_changeId(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol").resolve("GoodId");
        Files.createDirectories(bookDir);

        // Create images with wrong ID prefix
        Files.writeString(bookDir.resolve("BadId.001r.tif"), "image1");
        Files.writeString(bookDir.resolve("BadId.001v.tif"), "image2");

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        store.renameImages("testcol", "GoodId", true, false, errors);

        assertTrue(errors.isEmpty(), "Expected no errors: " + errors);
        assertTrue(Files.exists(bookDir.resolve("GoodId.001r.tif")));
        assertTrue(Files.exists(bookDir.resolve("GoodId.001v.tif")));
        assertFalse(Files.exists(bookDir.resolve("BadId.001r.tif")));
        assertFalse(Files.exists(bookDir.resolve("BadId.001v.tif")));
    }

    @Test
    void renameImages_errorsOnMissingFileMap(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol").resolve("TestBook");
        Files.createDirectories(bookDir);

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        store.renameImages("testcol", "TestBook", false, false, errors);

        assertFalse(errors.isEmpty(), "Should report missing file map");
        assertTrue(errors.stream().anyMatch(e -> e.contains("No file map found")));
    }

    @Test
    void renameImages_errorsOnDuplicateTargets(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol").resolve("TestBook");
        Files.createDirectories(bookDir);

        Files.writeString(bookDir.resolve("a.tif"), "image1");
        Files.writeString(bookDir.resolve("b.tif"), "image2");

        // Duplicate target: both map to same new name
        Files.writeString(bookDir.resolve("filemap.csv"),
                "a.tif,same.tif\nb.tif,same.tif\n");

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        store.renameImages("testcol", "TestBook", false, false, errors);

        assertFalse(errors.isEmpty(), "Should report duplicate targets");
        // Files should remain unchanged
        assertTrue(Files.exists(bookDir.resolve("a.tif")));
        assertTrue(Files.exists(bookDir.resolve("b.tif")));
    }

    @Test
    void renameTranscriptions_forwardRename(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol").resolve("TestBook");
        Files.createDirectories(bookDir);

        // Create a transcription XML that references an image
        String xmlContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <transcription>
                  <page filename="TestBook.001r.tif">
                    <annotation>Some text</annotation>
                  </page>
                </transcription>
                """;
        Files.writeString(bookDir.resolve("TestBook.aor.001r.xml"), xmlContent);

        // filemap maps old image -> new image
        Files.writeString(bookDir.resolve("filemap.csv"),
                "TestBook.001r.tif,NewBook.001r.tif\n");

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        store.renameTranscriptions("testcol", "TestBook", false, errors);

        assertTrue(errors.isEmpty(), "Expected no errors: " + errors);

        // The transcription file should be renamed
        Path newFile = bookDir.resolve("NewBook.aor.001r.xml");
        assertTrue(Files.exists(newFile), "Transcription should be renamed");
        assertFalse(Files.exists(bookDir.resolve("TestBook.aor.001r.xml")),
                "Old transcription file should not exist");

        // The page filename attribute should be updated
        String newContent = Files.readString(newFile);
        assertTrue(newContent.contains("NewBook.001r.tif"),
                "Page filename attribute should be updated");
        assertFalse(newContent.contains("TestBook.001r.tif"),
                "Old page filename should not remain");
    }

    @Test
    void renameTranscriptions_errorsOnMissingFileMap(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol").resolve("TestBook");
        Files.createDirectories(bookDir);

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        store.renameTranscriptions("testcol", "TestBook", false, errors);

        assertFalse(errors.isEmpty(), "Should report missing file map");
        assertTrue(errors.stream().anyMatch(e -> e.contains("No file map found")));
    }

    @Test
    void renameTranscriptions_errorsOnDuplicateTargets(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol").resolve("TestBook");
        Files.createDirectories(bookDir);

        String xmlContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <transcription><page filename="a.tif"/></transcription>
                """;
        Files.writeString(bookDir.resolve("TestBook.aor.001r.xml"), xmlContent);

        // Duplicate target values
        Files.writeString(bookDir.resolve("filemap.csv"),
                "a.tif,same.tif\nb.tif,same.tif\n");

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        store.renameTranscriptions("testcol", "TestBook", false, errors);

        assertFalse(errors.isEmpty(), "Should report duplicate targets");
    }

    @Test
    void generateFileMap_noDuplicateTargetNames(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol").resolve("TestBook");
        Files.createDirectories(bookDir);

        // Create 20 .tif files
        for (int i = 1; i <= 20; i++) {
            Files.writeString(bookDir.resolve(String.format("img%03d.tif", i)), "image");
        }

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        store.generateFileMap("testcol", "TestBook", "Book", true, true, 3, 3, 2, errors);

        assertTrue(errors.isEmpty(), "Expected no errors: " + errors);

        String content = Files.readString(bookDir.resolve("filemap.csv"));
        List<String> targetNames = content.lines()
                .filter(l -> !l.isBlank())
                .map(l -> l.split(",")[1])
                .toList();

        // All target names should be unique
        assertEquals(targetNames.size(), targetNames.stream().distinct().count(),
                "All target filenames should be unique");
    }

    @Test
    void renameImages_forwardThenReverse_roundTrip(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path bookDir = archiveDir.resolve("testcol").resolve("TestBook");
        Files.createDirectories(bookDir);

        // Create 5 .tif files with distinct content
        for (int i = 1; i <= 5; i++) {
            Files.writeString(bookDir.resolve(String.format("orig%03d.tif", i)), "content" + i);
        }

        var store = new FileSystemArchiveStore(archiveDir);
        List<String> errors = new ArrayList<>();

        // Generate file map
        store.generateFileMap("testcol", "TestBook", "NewId", false, false, 0, 0, 0, errors);
        assertTrue(errors.isEmpty(), "Expected no errors from generateFileMap: " + errors);

        // Rename forward
        store.renameImages("testcol", "TestBook", false, false, errors);
        assertTrue(errors.isEmpty(), "Expected no errors from forward rename: " + errors);

        // Verify originals are gone
        for (int i = 1; i <= 5; i++) {
            assertFalse(Files.exists(bookDir.resolve(String.format("orig%03d.tif", i))));
        }

        // Rename reverse
        store.renameImages("testcol", "TestBook", false, true, errors);
        assertTrue(errors.isEmpty(), "Expected no errors from reverse rename: " + errors);

        // Verify originals are back
        for (int i = 1; i <= 5; i++) {
            Path origPath = bookDir.resolve(String.format("orig%03d.tif", i));
            assertTrue(Files.exists(origPath), "Original should be restored: " + origPath);
            assertEquals("content" + i, Files.readString(origPath));
        }
    }
}
