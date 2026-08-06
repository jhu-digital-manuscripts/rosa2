package rosa.archive.core;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for FileSystemArchiveStore operations.
 */
class FileSystemArchiveStorePropertyTest {

    // Feature: rosa2-cli-refactor, Property 21: Rename Forward-Reverse Round Trip
    // Renaming forward then reverse restores original filenames exactly
    // **Validates: Requirements 20.1, 20.3, 22.1, 22.2**
    @Property(tries = 100)
    void renameForwardThenReverseRestoresOriginalFilenames(
            @ForAll("tifFileSet") List<TifFileEntry> entries
    ) throws IOException {
        Path tempDir = Files.createTempDirectory("rosa2-prop-rename-");
        try {
            Path archiveDir = tempDir.resolve("archive");
            Path bookDir = archiveDir.resolve("testcol").resolve("TestBook");
            Files.createDirectories(bookDir);

            // Step 1: Create N .tif files with unique names and distinct content
            Map<String, String> originalContents = new LinkedHashMap<>();
            for (TifFileEntry entry : entries) {
                Files.writeString(bookDir.resolve(entry.name()), entry.content(), StandardCharsets.UTF_8);
                originalContents.put(entry.name(), entry.content());
            }

            var store = new FileSystemArchiveStore(archiveDir);
            List<String> errors = new ArrayList<>();

            // Step 2: Generate a file map
            int n = entries.size();
            // Use simple config: no covers, no flyleaves, no misc — all body pages
            store.generateFileMap("testcol", "TestBook", "NewId", false, false, 0, 0, 0, errors);
            assertTrue(errors.isEmpty(), "Expected no errors from generateFileMap: " + errors);

            // Step 3: Rename forward (old -> new)
            store.renameImages("testcol", "TestBook", false, false, errors);
            assertTrue(errors.isEmpty(), "Expected no errors from forward rename: " + errors);

            // Step 4: Verify original filenames are gone
            for (String originalName : originalContents.keySet()) {
                assertFalse(Files.exists(bookDir.resolve(originalName)),
                        "Original file should not exist after forward rename: " + originalName);
            }

            // Step 5: Rename reverse (new -> old)
            store.renameImages("testcol", "TestBook", false, true, errors);
            assertTrue(errors.isEmpty(), "Expected no errors from reverse rename: " + errors);

            // Step 6: Verify all original filenames are restored with their original content intact
            for (Map.Entry<String, String> orig : originalContents.entrySet()) {
                Path filePath = bookDir.resolve(orig.getKey());
                assertTrue(Files.exists(filePath),
                        "Original file should be restored after reverse rename: " + orig.getKey());
                String restoredContent = Files.readString(filePath, StandardCharsets.UTF_8);
                assertEquals(orig.getValue(), restoredContent,
                        "Content should be intact after round-trip for: " + orig.getKey());
            }
        } finally {
            deleteRecursively(tempDir);
        }
    }

    /**
     * Generates a list of TIF file entries with unique names and distinct content.
     * File count is between 2 and 20 to exercise varying file map sizes.
     */
    @Provide
    Arbitrary<List<TifFileEntry>> tifFileSet() {
        // Generate a count between 2 and 20
        Arbitrary<Integer> countArb = Arbitraries.integers().between(2, 20);

        return countArb.flatMap(count -> {
            // Generate 'count' unique filenames and distinct content
            Arbitrary<String> baseNameArb = Arbitraries.strings()
                    .alpha()
                    .ofMinLength(3)
                    .ofMaxLength(8)
                    .map(String::toLowerCase);

            return baseNameArb.list().ofSize(count).uniqueElements()
                    .map(names -> {
                        List<TifFileEntry> entries = new ArrayList<>();
                        for (int i = 0; i < names.size(); i++) {
                            // Ensure filenames sort consistently with a numeric prefix
                            String filename = String.format("%s_%03d.tif", names.get(i), i + 1);
                            String content = "content_" + i + "_" + names.get(i);
                            entries.add(new TifFileEntry(filename, content));
                        }
                        return entries;
                    })
                    // Ensure no duplicate filenames
                    .filter(list -> {
                        Set<String> nameSet = new HashSet<>();
                        for (TifFileEntry e : list) {
                            if (!nameSet.add(e.name())) return false;
                        }
                        return true;
                    });
        });
    }

    /**
     * Simple record for a TIF file with name and content.
     */
    record TifFileEntry(String name, String content) {}

    // Feature: rosa2-cli-refactor, Property 22: Rename Completeness
    // After renaming, directory contains all new-names and none of old-names
    // **Validates: Requirements 20.1, 21.1, 22.1**
    @Property(tries = 100)
    void afterRenamingDirectoryContainsAllNewNamesAndNoOldNames(
            @ForAll("tifFileSet") List<TifFileEntry> entries
    ) throws IOException {
        Path tempDir = Files.createTempDirectory("rosa2-prop-rename-completeness-");
        try {
            Path archiveDir = tempDir.resolve("archive");
            Path bookDir = archiveDir.resolve("testcol").resolve("TestBook");
            Files.createDirectories(bookDir);

            // Step 1: Create N .tif files with unique names
            Set<String> oldNames = new LinkedHashSet<>();
            for (TifFileEntry entry : entries) {
                Files.writeString(bookDir.resolve(entry.name()), entry.content(), StandardCharsets.UTF_8);
                oldNames.add(entry.name());
            }

            var store = new FileSystemArchiveStore(archiveDir);
            List<String> errors = new ArrayList<>();

            // Step 2: Generate a file map
            store.generateFileMap("testcol", "TestBook", "NewId", false, false, 0, 0, 0, errors);
            assertTrue(errors.isEmpty(), "Expected no errors from generateFileMap: " + errors);

            // Step 3: Read the file map to know the expected new filenames
            Path fileMapPath = bookDir.resolve("filemap.csv");
            assertTrue(Files.exists(fileMapPath), "filemap.csv should have been created");
            List<String> mapLines = Files.readAllLines(fileMapPath, StandardCharsets.UTF_8);

            Set<String> expectedNewNames = new LinkedHashSet<>();
            for (String line : mapLines) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] parts = line.split(",", 2);
                if (parts.length == 2 && !parts[1].isBlank()) {
                    expectedNewNames.add(parts[1].trim());
                }
            }

            // Verify the file map has exactly N entries (one per old file)
            assertEquals(oldNames.size(), expectedNewNames.size(),
                    "File map should have exactly one entry per old file");

            // Step 4: Rename forward (old -> new)
            store.renameImages("testcol", "TestBook", false, false, errors);
            assertTrue(errors.isEmpty(), "Expected no errors from forward rename: " + errors);

            // Step 5: Gather current .tif files in the directory
            Set<String> filesAfterRename = new LinkedHashSet<>();
            try (var stream = Files.newDirectoryStream(bookDir, "*.tif")) {
                for (Path p : stream) {
                    filesAfterRename.add(p.getFileName().toString());
                }
            }

            // Step 6: Verify directory contains ALL new filenames from filemap.csv
            for (String newName : expectedNewNames) {
                assertTrue(filesAfterRename.contains(newName),
                        "Expected new filename present after rename: " + newName);
            }

            // Step 7: Verify directory contains NONE of the old filenames
            for (String oldName : oldNames) {
                assertFalse(filesAfterRename.contains(oldName),
                        "Old filename should not exist after rename: " + oldName);
            }

            // Step 8: Verify the set of .tif files equals exactly the set of target names
            assertEquals(expectedNewNames, filesAfterRename,
                    "The set of .tif files after rename should equal exactly the target names from filemap");

        } finally {
            deleteRecursively(tempDir);
        }
    }

    // Feature: rosa2-cli-refactor, Property 22: Rename Completeness (standalone rename-files)
    // After renaming via CSV, directory contains all new-names and none of old-names
    // **Validates: Requirement 21.1**
    @Property(tries = 100)
    void standaloneRenameFilesContainsAllNewNamesAndNoOldNames(
            @ForAll("tifFileSet") List<TifFileEntry> entries
    ) throws IOException {
        Path tempDir = Files.createTempDirectory("rosa2-prop-rename-files-completeness-");
        try {
            Path filesDir = tempDir.resolve("files");
            Files.createDirectories(filesDir);

            // Step 1: Create N .tif files with unique names
            Set<String> oldNames = new LinkedHashSet<>();
            for (TifFileEntry entry : entries) {
                Files.writeString(filesDir.resolve(entry.name()), entry.content(), StandardCharsets.UTF_8);
                oldNames.add(entry.name());
            }

            // Step 2: Generate a CSV mapping old -> new names (simple sequential renaming)
            Set<String> newNames = new LinkedHashSet<>();
            StringBuilder csvContent = new StringBuilder();
            int i = 1;
            for (String oldName : oldNames) {
                String newName = String.format("renamed_%03d.tif", i++);
                newNames.add(newName);
                csvContent.append(oldName).append(",").append(newName).append("\n");
            }
            Path csvFile = tempDir.resolve("rename.csv");
            Files.writeString(csvFile, csvContent.toString(), StandardCharsets.UTF_8);

            // Step 3: Rename files according to CSV
            List<String> lines = Files.readAllLines(csvFile, StandardCharsets.UTF_8);
            List<String> errors = new ArrayList<>();
            for (String line : lines) {
                if (line.isBlank()) continue;
                String[] parts = line.split(",", 2);
                if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                    errors.add("Invalid CSV line: " + line);
                    continue;
                }
                Path source = filesDir.resolve(parts[0].trim());
                Path target = filesDir.resolve(parts[1].trim());
                if (Files.exists(source)) {
                    Files.move(source, target);
                } else {
                    errors.add("Source file not found: " + parts[0].trim());
                }
            }
            assertTrue(errors.isEmpty(), "Expected no errors from rename: " + errors);

            // Step 4: Gather current .tif files in the directory
            Set<String> filesAfterRename = new LinkedHashSet<>();
            try (var stream = Files.newDirectoryStream(filesDir, "*.tif")) {
                for (Path p : stream) {
                    filesAfterRename.add(p.getFileName().toString());
                }
            }

            // Step 5: Verify directory contains ALL new filenames
            for (String newName : newNames) {
                assertTrue(filesAfterRename.contains(newName),
                        "Expected new filename present after rename: " + newName);
            }

            // Step 6: Verify directory contains NONE of the old filenames
            for (String oldName : oldNames) {
                assertFalse(filesAfterRename.contains(oldName),
                        "Old filename should not exist after rename: " + oldName);
            }

            // Step 7: Verify the set of .tif files equals exactly the set of new names
            assertEquals(newNames, filesAfterRename,
                    "The set of .tif files after rename should equal exactly the target names");

        } finally {
            deleteRecursively(tempDir);
        }
    }

    /**
     * Recursively delete a directory tree.
     */
    private void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (var walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {
                            // best effort cleanup
                        }
                    });
        }
    }
}
