package rosa.archive.core;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;
import org.junit.jupiter.api.io.TempDir;
import rosa.archive.core.util.HashUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Property-based test for checksum round-trip integrity.
 *
 * <p>Feature: rosa2-cli-refactor, Property 17: Checksum Round-Trip Integrity
 *
 * <p>After running {@code update --force}, the stored checksum for every file
 * equals {@code HashUtil.computeSHA1()} applied to the current file content.
 */
class ChecksumRoundTripPropertyTest {

    // We need a @TempDir per property invocation. jqwik doesn't support JUnit's @TempDir
    // directly on @Property methods, so we manage temp directories manually.

    @Property(tries = 100)
    void checksumRoundTrip_forcedUpdateMatchesComputedHash(
            @ForAll("fileContentsSet") List<byte[]> fileContentsList
    ) throws IOException {
        // Create a temporary directory for this test run
        Path tempDir = Files.createTempDirectory("checksum-prop-test");
        try {
            // Set up archive structure: archive/collection/book/
            Path archiveDir = tempDir.resolve("archive");
            Path collectionDir = archiveDir.resolve("testcol");
            Path bookDir = collectionDir.resolve("testbook");
            Files.createDirectories(bookDir);

            // Create files with the generated content
            List<String> fileNames = new ArrayList<>();
            for (int i = 0; i < fileContentsList.size(); i++) {
                String fileName = "file" + i + ".xml";
                fileNames.add(fileName);
                Files.write(bookDir.resolve(fileName), fileContentsList.get(i));
            }

            // Run updateChecksum with force=true
            FileSystemArchiveStore store = new FileSystemArchiveStore(archiveDir);
            List<String> errors = new ArrayList<>();
            store.updateChecksum("testcol", "testbook", true, errors);

            // Verify no errors occurred
            if (!errors.isEmpty()) {
                throw new AssertionError("Unexpected errors: " + errors);
            }

            // Read the SHA1SUM file
            Path checksumFile = bookDir.resolve("testbook.SHA1SUM");
            if (!Files.exists(checksumFile)) {
                throw new AssertionError("SHA1SUM file was not created");
            }

            // Parse the checksum file: format is "hash  filename"
            Map<String, String> storedChecksums = parseChecksumFile(checksumFile);

            // Verify every file has an entry and its stored hash matches a fresh computation
            for (String fileName : fileNames) {
                String storedHash = storedChecksums.get(fileName);
                if (storedHash == null) {
                    throw new AssertionError("No checksum entry for file: " + fileName);
                }

                String computedHash = HashUtil.computeSHA1(bookDir.resolve(fileName));
                if (!storedHash.equals(computedHash)) {
                    throw new AssertionError(
                            "Checksum mismatch for " + fileName +
                                    ": stored=" + storedHash + ", computed=" + computedHash);
                }
            }

            // Also verify the checksum file doesn't contain extra entries
            // (it should only have entries for the data files, not itself)
            if (storedChecksums.size() != fileNames.size()) {
                throw new AssertionError(
                        "Expected " + fileNames.size() + " entries but found " + storedChecksums.size());
            }
        } finally {
            // Clean up temp directory
            deleteRecursively(tempDir);
        }
    }

    @Provide
    Arbitrary<List<byte[]>> fileContentsSet() {
        // Generate 1-10 files, each with 0 to 1024 bytes of arbitrary content
        Arbitrary<byte[]> fileContent = Arbitraries.bytes()
                .array(byte[].class)
                .ofMinSize(0)
                .ofMaxSize(1024);

        return fileContent.list().ofMinSize(1).ofMaxSize(10);
    }

    /**
     * Parses a SHA1SUM file into a map of filename → hash.
     * Format is: {@code <hash>  <filename>} (two spaces between hash and filename).
     */
    private Map<String, String> parseChecksumFile(Path checksumFile) throws IOException {
        List<String> lines = Files.readAllLines(checksumFile, StandardCharsets.UTF_8);
        Map<String, String> result = new java.util.HashMap<>();
        for (String line : lines) {
            if (line.isBlank()) continue;
            String[] parts = line.split("\\s+", 2);
            if (parts.length == 2) {
                result.put(parts[1], parts[0]);
            }
        }
        return result;
    }

    /**
     * Recursively deletes a directory and all its contents.
     */
    private void deleteRecursively(Path path) throws IOException {
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
