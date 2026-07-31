package rosa.archive.core;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Property-based tests for file map generation in FileSystemArchiveStore.
 *
 * Feature: rosa2-cli-refactor, Property 20: File Map Naming Convention
 *
 * For any sorted list of N image files and a valid configuration (front cover, back cover,
 * frontmatter count, endmatter count, misc count), the generated file map SHALL contain
 * exactly N rows, each target filename SHALL follow the archive naming convention, and
 * no two target filenames SHALL be identical.
 *
 * Validates: Requirements 19.1, 19.2
 */
class FileSystemArchiveStoreFileMapPropertyTest {

    // Patterns for valid target filenames in the archive naming convention
    private static final Pattern FRONT_COVER = Pattern.compile(".*\\.binding\\.frontcover\\.tif$");
    private static final Pattern BACK_COVER = Pattern.compile(".*\\.binding\\.backcover\\.tif$");
    private static final Pattern FRONT_PASTEDOWN = Pattern.compile(".*\\.frontmatter\\.pastedown\\.tif$");
    private static final Pattern END_PASTEDOWN = Pattern.compile(".*\\.endmatter\\.pastedown\\.tif$");
    private static final Pattern FRONT_FLYLEAF = Pattern.compile(".*\\.frontmatter\\.flyleaf\\.\\d{3}[rv]\\.tif$");
    private static final Pattern END_FLYLEAF = Pattern.compile(".*\\.endmatter\\.flyleaf\\.\\d{3}[rv]\\.tif$");
    private static final Pattern BODY_PAGE = Pattern.compile(".*\\.\\d{3}[rv]\\.tif$");
    private static final Pattern MISC = Pattern.compile(".*\\.misc\\.\\d{3}\\.tif$");

    /**
     * Combined pattern: any valid archive naming convention filename must match
     * one of the known categories.
     */
    private static boolean matchesNamingConvention(String filename) {
        return FRONT_COVER.matcher(filename).matches()
                || BACK_COVER.matcher(filename).matches()
                || FRONT_PASTEDOWN.matcher(filename).matches()
                || END_PASTEDOWN.matcher(filename).matches()
                || FRONT_FLYLEAF.matcher(filename).matches()
                || END_FLYLEAF.matcher(filename).matches()
                || BODY_PAGE.matcher(filename).matches()
                || MISC.matcher(filename).matches();
    }

    @Property(tries = 100)
    void fileMapHasExactlyNRowsAllUniqueAndFollowNamingConvention(
            @ForAll("validFileMapConfig") FileMapConfig config) throws IOException {

        // Set up temp directory structure
        Path tempDir = Files.createTempDirectory("filemap-property-test");
        try {
            Path archiveDir = tempDir.resolve("archive");
            Path bookDir = archiveDir.resolve("testcol").resolve("TestBook");
            Files.createDirectories(bookDir);

            // Create N .tif files
            for (int i = 1; i <= config.numFiles(); i++) {
                Files.writeString(bookDir.resolve(String.format("img%04d.tif", i)), "image",
                        StandardCharsets.UTF_8);
            }

            var store = new FileSystemArchiveStore(archiveDir);
            var errors = new java.util.ArrayList<String>();

            store.generateFileMap("testcol", "TestBook", "BookId",
                    config.hasFrontCover(), config.hasBackCover(),
                    config.numFrontmatter(), config.numEndmatter(), config.numMisc(), errors);

            // Verify no errors during generation
            assert errors.isEmpty() : "Unexpected errors: " + errors;

            // Read the generated filemap.csv
            Path fileMapPath = bookDir.resolve("filemap.csv");
            assert Files.exists(fileMapPath) : "filemap.csv should exist";

            String content = Files.readString(fileMapPath, StandardCharsets.UTF_8);
            List<String> lines = content.lines().filter(l -> !l.isBlank()).toList();

            // Property: exactly N rows for N image files
            assert lines.size() == config.numFiles()
                    : "Expected " + config.numFiles() + " rows but got " + lines.size();

            Set<String> targetNames = new HashSet<>();
            for (String line : lines) {
                String[] parts = line.split(",", 2);
                assert parts.length == 2 : "Each line should have old,new format: " + line;

                String targetFilename = parts[1].trim();

                // Property: all target filenames end with .tif
                assert targetFilename.endsWith(".tif")
                        : "Target filename should end with .tif: " + targetFilename;

                // Property: all target filenames follow the naming convention
                assert matchesNamingConvention(targetFilename)
                        : "Target filename does not follow naming convention: " + targetFilename;

                // Collect for uniqueness check
                targetNames.add(targetFilename);
            }

            // Property: no duplicate target filenames
            assert targetNames.size() == config.numFiles()
                    : "Expected " + config.numFiles() + " unique targets but got " + targetNames.size()
                    + " (duplicates found)";

        } finally {
            // Clean up temp directory
            deleteRecursively(tempDir);
        }
    }

    @Provide
    Arbitrary<FileMapConfig> validFileMapConfig() {
        return Combinators.combine(
                Arbitraries.integers().between(4, 30),  // numFiles: between 4 and 30
                Arbitraries.of(true, false),             // hasFrontCover
                Arbitraries.of(true, false)              // hasBackCover
        ).flatAs((numFiles, hasFrontCover, hasBackCover) -> {
            // Calculate how many files are consumed by covers
            int coverFiles = (hasFrontCover ? 2 : 0) + (hasBackCover ? 2 : 0);
            int remaining = numFiles - coverFiles;

            // We need at least 1 body page, so remaining must be > frontmatter + endmatter + misc
            // Constrain frontmatter, endmatter, misc so they don't exceed remaining - 1
            // (need at least 1 body page)
            int maxSpecial = Math.max(0, remaining - 1);

            return Arbitraries.integers().between(0, Math.min(maxSpecial, 10))
                    .flatMap(numFrontmatter -> {
                        int remainingAfterFront = maxSpecial - numFrontmatter;
                        return Arbitraries.integers().between(0, Math.min(remainingAfterFront, 10))
                                .flatMap(numEndmatter -> {
                                    int remainingAfterEnd = remainingAfterFront - numEndmatter;
                                    return Arbitraries.integers().between(0, Math.min(remainingAfterEnd, 10))
                                            .map(numMisc -> new FileMapConfig(
                                                    numFiles, hasFrontCover, hasBackCover,
                                                    numFrontmatter, numEndmatter, numMisc));
                                });
                    });
        });
    }

    record FileMapConfig(int numFiles, boolean hasFrontCover, boolean hasBackCover,
                         int numFrontmatter, int numEndmatter, int numMisc) {
        @Override
        public String toString() {
            return "FileMapConfig[files=" + numFiles + ", frontCover=" + hasFrontCover
                    + ", backCover=" + hasBackCover + ", frontmatter=" + numFrontmatter
                    + ", endmatter=" + numEndmatter + ", misc=" + numMisc + "]";
        }
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
