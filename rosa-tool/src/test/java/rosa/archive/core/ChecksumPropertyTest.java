package rosa.archive.core;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;
import rosa.archive.core.util.HashUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Property-based tests for checksum update operations in FileSystemArchiveStore.
*/
class ChecksumPropertyTest {

    /**
     * Property 18: Non-Forced Checksum Stability
     *
     * Files with lastModifiedTime not newer than the SHA1SUM file remain unchanged
     * after running `update` without `--force`.
     *
     * Test approach:
     * 1. Create a book directory with generated files
     * 2. Run forced checksum update to create the SHA1SUM file
     * 3. Replace hash entries in SHA1SUM with fake hashes (preserving filenames)
     * 4. Set SHA1SUM lastModifiedTime newer than all data files
     * 5. Run non-forced checksum update
     * 6. Verify the fake hashes are preserved (since data files are not newer than SHA1SUM)
     */
    @Property(tries = 100)
    void nonForcedUpdatePreservesHashesForUnmodifiedFiles(
            @ForAll("bookFileContents") List<Map.Entry<String, String>> fileEntries
    ) throws IOException {
        Path tempDir = Files.createTempDirectory("checksum-prop-");
        try {
            // Set up archive/collection/book structure
            Path archiveDir = tempDir.resolve("archive");
            Path collectionDir = archiveDir.resolve("testcol");
            Path bookDir = collectionDir.resolve("testbook");
            Files.createDirectories(bookDir);

            // Step 1: Create book files with generated content
            for (var entry : fileEntries) {
                Files.writeString(bookDir.resolve(entry.getKey()), entry.getValue(),
                        StandardCharsets.UTF_8);
            }

            FileSystemArchiveStore store = new FileSystemArchiveStore(archiveDir);
            List<String> errors = new ArrayList<>();

            // Step 2: Run forced checksum update to create a valid SHA1SUM
            store.updateChecksum("testcol", "testbook", true, errors);
            assert errors.isEmpty() : "Forced update should not produce errors: " + errors;

            Path checksumFile = bookDir.resolve("testbook.SHA1SUM");
            assert Files.exists(checksumFile) : "SHA1SUM file should exist after forced update";

            // Step 3: Replace all hashes with fake hashes while preserving the format
            String originalContent = Files.readString(checksumFile, StandardCharsets.UTF_8);
            StringBuilder fakeContent = new StringBuilder();
            String[] lines = originalContent.split("\n");
            for (String line : lines) {
                if (line.isBlank()) continue;
                // Format is "hash  filename" — replace hash with a fake one
                int separatorIdx = line.indexOf("  ");
                if (separatorIdx > 0) {
                    String fileName = line.substring(separatorIdx + 2);
                    String fakeHash = "a".repeat(40); // 40-char fake hash
                    fakeContent.append(fakeHash).append("  ").append(fileName).append('\n');
                }
            }
            Files.writeString(checksumFile, fakeContent.toString(), StandardCharsets.UTF_8);

            // Step 4: Set SHA1SUM file time to be newer than all data files
            long now = System.currentTimeMillis();
            FileTime futureTime = FileTime.fromMillis(now + 60_000);
            FileTime pastTime = FileTime.fromMillis(now - 60_000);

            Files.setLastModifiedTime(checksumFile, futureTime);
            for (var entry : fileEntries) {
                Files.setLastModifiedTime(bookDir.resolve(entry.getKey()), pastTime);
            }

            // Step 5: Run non-forced checksum update
            errors.clear();
            store.updateChecksum("testcol", "testbook", false, errors);
            assert errors.isEmpty() : "Non-forced update should not produce errors: " + errors;

            // Step 6: Verify fake hashes are preserved
            String resultContent = Files.readString(checksumFile, StandardCharsets.UTF_8);
            for (var entry : fileEntries) {
                String expectedLine = "a".repeat(40) + "  " + entry.getKey();
                assert resultContent.contains(expectedLine) :
                        "Expected fake hash to be preserved for file '" + entry.getKey()
                                + "' but got:\n" + resultContent;
            }
        } finally {
            deleteRecursively(tempDir);
        }
    }

    @Provide
    Arbitrary<List<Map.Entry<String, String>>> bookFileContents() {
        // Generate between 1 and 5 files with distinct names and random content
        Arbitrary<String> fileNames = Arbitraries.of(
                "page001.xml", "page002.xml", "page003.xml",
                "metadata.csv", "notes.txt", "data.html",
                "image_list.csv", "transcription.xml", "config.txt"
        );

        Arbitrary<String> fileContent = Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(200);

        Arbitrary<Map.Entry<String, String>> fileEntry = Combinators.combine(fileNames, fileContent)
                .as(Map::entry);

        return fileEntry.list()
                .ofMinSize(1)
                .ofMaxSize(5)
                .uniqueElements(Map.Entry::getKey);
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var entries = Files.list(path)) {
                for (Path entry : entries.toList()) {
                    deleteRecursively(entry);
                }
            }
        }
        Files.deleteIfExists(path);
    }
}
