package rosa.archive.core;

import net.jqwik.api.*;
import net.jqwik.api.constraints.Size;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Property 19: Image List Completeness
 * Generated image list CSV contains exactly one entry per .tif/.jpg file and no entries for non-image files.
 *
 * Validates: Requirements 17.1
 */
class ImageListCompletenessPropertyTest {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".tif", ".jpg");
    private static final List<String> NON_IMAGE_EXTENSIONS = List.of(".xml", ".csv", ".txt", ".html", ".pdf", ".md");

    // Feature: rosa2-cli-refactor, Property 19: Image List Completeness
    @Property(tries = 100)
    void imageListContainsExactlyOneEntryPerImageFile(
            @ForAll("filenameSets") Set<String> filenames
    ) throws IOException {
        // Create temp directory structure
        Path tempDir = Files.createTempDirectory("imagelist-prop-");
        try {
            Path archiveDir = tempDir.resolve("archive");
            Path bookDir = archiveDir.resolve("col").resolve("book");
            Files.createDirectories(bookDir);

            Set<String> expectedImageFiles = new TreeSet<>();
            Set<String> nonImageFiles = new TreeSet<>();

            // Create files in the book directory
            for (String filename : filenames) {
                Path filePath = bookDir.resolve(filename);
                String lower = filename.toLowerCase();
                if (lower.endsWith(".tif") || lower.endsWith(".jpg")) {
                    // Write a valid image file
                    writeTestImage(filePath, 64, 48);
                    expectedImageFiles.add(filename);
                } else {
                    // Write a non-image file
                    Files.writeString(filePath, "non-image content");
                    nonImageFiles.add(filename);
                }
            }

            // Run generateAndWriteImageList
            var store = new FileSystemArchiveStore(archiveDir);
            List<String> errors = new ArrayList<>();
            store.generateAndWriteImageList("col", "book", true, errors);

            // Read the resulting CSV
            Path imagesCsv = bookDir.resolve("book.images.csv");

            if (expectedImageFiles.isEmpty()) {
                // If no images, CSV should exist but be empty
                assert Files.exists(imagesCsv) : "images.csv should exist even when no images are present";
                String content = Files.readString(imagesCsv);
                assert content.isEmpty() : "images.csv should be empty when no image files exist, but was: " + content;
                return;
            }

            assert Files.exists(imagesCsv) : "images.csv should be created";

            List<String> lines = Files.readAllLines(imagesCsv, StandardCharsets.UTF_8);

            // Extract filenames from CSV (first column before comma)
            Set<String> csvFilenames = lines.stream()
                    .map(line -> line.split(",")[0])
                    .collect(Collectors.toSet());

            // Property: exactly one entry per image file
            assert csvFilenames.size() == expectedImageFiles.size() :
                    "Expected " + expectedImageFiles.size() + " entries but got " + csvFilenames.size() +
                            ". Expected: " + expectedImageFiles + ", Got: " + csvFilenames;

            // Property: all entries match actual image files
            assert csvFilenames.equals(expectedImageFiles) :
                    "CSV entries don't match image files. Missing: " +
                            diff(expectedImageFiles, csvFilenames) + ", Extra: " + diff(csvFilenames, expectedImageFiles);

            // Property: no entries for non-image files
            for (String nonImage : nonImageFiles) {
                assert !csvFilenames.contains(nonImage) :
                        "Non-image file '" + nonImage + "' should not appear in the image list";
            }

            // Property: each line has exactly one entry (no duplicates)
            assert lines.size() == csvFilenames.size() :
                    "Duplicate entries detected. Lines: " + lines.size() + ", Unique filenames: " + csvFilenames.size();

        } finally {
            deleteRecursive(tempDir);
        }
    }

    @Provide
    Arbitrary<Set<String>> filenameSets() {
        // Generate basenames that are valid filenames (alphanumeric + underscore, no dots except extension)
        Arbitrary<String> baseName = Arbitraries.strings()
                .ofMinLength(1)
                .ofMaxLength(12)
                .alpha()
                .map(s -> s.isEmpty() ? "f" : s);

        // Image extensions
        Arbitrary<String> imageExt = Arbitraries.of(".tif", ".jpg");

        // Non-image extensions
        Arbitrary<String> nonImageExt = Arbitraries.of(".xml", ".csv", ".txt", ".html", ".pdf", ".md");

        // Generate image filenames
        Arbitrary<String> imageFilename = Combinators.combine(baseName, imageExt)
                .as((base, ext) -> base + ext);

        // Generate non-image filenames
        Arbitrary<String> nonImageFilename = Combinators.combine(baseName, nonImageExt)
                .as((base, ext) -> base + ext);

        // Mix image and non-image filenames into a set (1 to 15 items)
        return Arbitraries.of(imageFilename, nonImageFilename)
                .flatMap(arb -> arb)
                .set()
                .ofMinSize(1)
                .ofMaxSize(15);
    }

    /**
     * Writes a test image file at the given path.
     * Uses PNG format for .tif files (TIFF writer may not be available),
     * and JPEG format for .jpg files.
     */
    private void writeTestImage(Path path, int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        String filename = path.getFileName().toString().toLowerCase();
        String format;
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            format = "jpg";
        } else {
            format = "png";
        }
        ImageIO.write(img, format, path.toFile());
    }

    private Set<String> diff(Set<String> a, Set<String> b) {
        Set<String> result = new TreeSet<>(a);
        result.removeAll(b);
        return result;
    }

    private void deleteRecursive(Path path) {
        try {
            if (Files.isDirectory(path)) {
                try (var entries = Files.list(path)) {
                    entries.forEach(this::deleteRecursive);
                }
            }
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // Best effort cleanup
        }
    }
}
