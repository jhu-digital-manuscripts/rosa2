package rosa.archive.aor;

import net.jqwik.api.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for annotation map completeness.
 *
 * <p>Feature: rosa2-cli-refactor, Property 25: Annotation Map Completeness</p>
 * <p>Generated {@code id_locations.csv} contains an entry for every annotation ID
 * with correct collection/book/page information.</p>
  */
class AnnotationMapGeneratorPropertyTest {

    private static final String[] ANNOTATION_TYPES = {
            "marginalia", "underline", "mark", "symbol", "drawing",
            "errata", "numeral", "graph", "table", "calculation", "physical_link"
    };

    /**
     * Property 25: Annotation Map Completeness — Generated id_locations.csv contains entry
     * for every annotation ID with correct location.
     */
    @Property(tries = 50)
    void annotationMapContainsEntryForEveryAnnotationId(
            @ForAll("bookConfigs") List<BookConfig> books
    ) throws IOException {
        String collectionId = "TestCollection";
        Path tempDir = Files.createTempDirectory("annotmap-prop-");
        try {
            Path archiveDir = tempDir.resolve("archive");
            Path collectionDir = archiveDir.resolve(collectionId);
            Files.createDirectories(collectionDir);

            // Track all expected annotation IDs with their expected locations
            Map<String, ExpectedLocation> expectedAnnotations = new LinkedHashMap<>();

            // Create book directories with AoR transcription XML files
            for (BookConfig book : books) {
                Path bookDir = collectionDir.resolve(book.bookId());
                Files.createDirectories(bookDir);

                for (PageConfig page : book.pages()) {
                    String xmlContent = buildAorXml(page);
                    Path xmlFile = bookDir.resolve(page.transcriptionFilename());
                    Files.writeString(xmlFile, xmlContent, StandardCharsets.UTF_8);

                    // Record expected annotations
                    String pageLabel = derivePageLabel(book.bookId(), page.imageFilename());
                    for (String annotationId : page.annotationIds()) {
                        expectedAnnotations.put(annotationId,
                                new ExpectedLocation(collectionId, book.bookId(), pageLabel, annotationId));
                    }
                }
            }

            // Run the AnnotationMapGenerator
            var generator = new AnnotationMapGenerator(archiveDir);
            List<String> errors = generator.run(collectionId);

            // The generator should not produce fatal errors for well-formed XML
            List<String> fatalErrors = errors.stream()
                    .filter(e -> e.contains("Failed to parse"))
                    .toList();
            assertTrue(fatalErrors.isEmpty(),
                    "Generator should not fail on well-formed XML: " + fatalErrors);

            // Read the resulting id_locations.csv
            Path csvFile = collectionDir.resolve("id_locations.csv");
            assertTrue(Files.exists(csvFile), "id_locations.csv should be generated");

            List<String> lines = Files.readAllLines(csvFile, StandardCharsets.UTF_8);
            Map<String, String[]> csvEntries = new LinkedHashMap<>();
            for (String line : lines) {
                if (line.isBlank()) continue;
                String[] parts = line.split(",", -1);
                // Format: id,collection,book,page,annotation
                if (parts.length >= 5) {
                    csvEntries.put(parts[0], parts);
                }
            }

            // Property: every annotation ID from the generated XML is present in id_locations.csv
            for (Map.Entry<String, ExpectedLocation> entry : expectedAnnotations.entrySet()) {
                String annotationId = entry.getKey();
                ExpectedLocation expected = entry.getValue();

                assertTrue(csvEntries.containsKey(annotationId),
                        "Annotation ID '" + annotationId + "' should be present in id_locations.csv but was not found. "
                                + "CSV keys: " + csvEntries.keySet());

                String[] row = csvEntries.get(annotationId);
                assertEquals(expected.collection(), row[1],
                        "Collection mismatch for annotation '" + annotationId + "'");
                assertEquals(expected.book(), row[2],
                        "Book mismatch for annotation '" + annotationId + "'");
                assertEquals(expected.page(), row[3],
                        "Page mismatch for annotation '" + annotationId + "'");
                assertEquals(expected.annotation(), row[4],
                        "Annotation field mismatch for annotation '" + annotationId + "'");
            }

        } finally {
            deleteRecursively(tempDir);
        }
    }

    /**
     * Provides a list of 1-3 books, each with 1-4 pages, each with 1-5 annotations.
     * Book IDs are guaranteed unique across the list.
     */
    @Provide
    Arbitrary<List<BookConfig>> bookConfigs() {
        return Arbitraries.integers().between(1, 3).flatMap(count -> {
            Arbitrary<List<BookConfig>> result = Arbitraries.just(new ArrayList<>());
            for (int i = 0; i < count; i++) {
                String bookId = "Book" + (char) ('A' + i);
                Arbitrary<BookConfig> bookArb = bookConfigArbitrary(bookId);
                result = result.flatMap(list ->
                        bookArb.map(book -> {
                            List<BookConfig> newList = new ArrayList<>(list);
                            newList.add(book);
                            return newList;
                        }));
            }
            return result;
        });
    }

    private Arbitrary<BookConfig> bookConfigArbitrary(String bookId) {
        Arbitrary<List<PageConfig>> pages = pageConfigArbitrary(bookId)
                .list().ofMinSize(1).ofMaxSize(4)
                .filter(list -> hasUniqueFilenames(list));
        return pages.map(p -> new BookConfig(bookId, p));
    }

    private Arbitrary<PageConfig> pageConfigArbitrary(String bookId) {
        Arbitrary<String> pageNumbers = Arbitraries.integers()
                .between(1, 999)
                .map(n -> String.format("%03d", n));
        Arbitrary<String> pageSides = Arbitraries.of("r", "v");

        return Combinators.combine(pageNumbers, pageSides).flatAs((num, side) -> {
            String imageFilename = bookId + "." + num + side + ".tif";
            String transcriptionFilename = bookId + ".aor." + num + side + ".xml";

            // Generate 1-5 annotation IDs per page
            Arbitrary<List<AnnotationEntry>> annotationEntries = annotationEntryArbitrary(bookId, num + side)
                    .list().ofMinSize(1).ofMaxSize(5)
                    .filter(entries -> hasUniqueIds(entries));

            return annotationEntries.map(entries -> new PageConfig(
                    imageFilename,
                    transcriptionFilename,
                    entries.stream().map(AnnotationEntry::id).toList(),
                    entries.stream().map(AnnotationEntry::type).toList()
            ));
        });
    }

    private Arbitrary<AnnotationEntry> annotationEntryArbitrary(String bookId, String pageSuffix) {
        Arbitrary<String> types = Arbitraries.of(ANNOTATION_TYPES);
        Arbitrary<String> ids = Arbitraries.integers()
                .between(1, 9999)
                .map(n -> bookId + "_" + pageSuffix + "_" + String.format("%04d", n));
        return Combinators.combine(ids, types).as(AnnotationEntry::new);
    }

    private boolean hasUniqueFilenames(List<PageConfig> pages) {
        Set<String> filenames = pages.stream()
                .map(PageConfig::transcriptionFilename)
                .collect(Collectors.toSet());
        return filenames.size() == pages.size();
    }

    private boolean hasUniqueIds(List<AnnotationEntry> entries) {
        Set<String> ids = entries.stream()
                .map(AnnotationEntry::id)
                .collect(Collectors.toSet());
        return ids.size() == entries.size();
    }

    /**
     * Derives the page label as the AnnotationMapGenerator would for a filename with
     * format "name.ext" (removes the extension).
     */
    private String derivePageLabel(String bookId, String imageFilename) {
        // The getPageLabel method in AnnotationMapGenerator:
        // splits on "." — if 2 parts, returns parts[0] (strips extension)
        // Otherwise uses ArchiveNameParser.shortName
        String[] parts = imageFilename.split("\\.");
        if (parts.length == 2) {
            return parts[0];
        }
        // For multi-dot filenames (e.g., "BookXyz.001r.tif"), the split produces >2 parts
        // AnnotationMapGenerator falls through to shortName.
        // For our test, we use the NAME_PARSER shortName logic.
        // However, given our filename format "BookXyz.001r.tif" (3 parts),
        // the shortName will be used. Let's simulate it simply by using
        // the ArchiveNameParser directly.
        var parser = new rosa.archive.core.ArchiveNameParser();
        return parser.shortName(imageFilename);
    }

    /**
     * Builds a minimal AoR transcription XML containing annotations with IDs.
     */
    private String buildAorXml(PageConfig page) {
        StringBuilder annotations = new StringBuilder();
        for (int i = 0; i < page.annotationIds().size(); i++) {
            String id = page.annotationIds().get(i);
            String type = page.annotationTypes().get(i);
            annotations.append("      <").append(type).append(" id=\"")
                    .append(escapeXml(id)).append("\"/>\n");
        }

        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <transcription xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                  <page filename="%s" reader="Harvey"/>
                  <annotation>
                %s  </annotation>
                </transcription>
                """.formatted(page.imageFilename(), annotations.toString());
    }

    private String escapeXml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

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

    // --- Internal records for test data ---

    record BookConfig(String bookId, List<PageConfig> pages) {}

    record PageConfig(String imageFilename, String transcriptionFilename,
                      List<String> annotationIds, List<String> annotationTypes) {}

    record AnnotationEntry(String id, String type) {}

    record ExpectedLocation(String collection, String book, String page, String annotation) {}
}
