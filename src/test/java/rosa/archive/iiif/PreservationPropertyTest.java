package rosa.archive.iiif;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.lifecycle.BeforeTry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import rosa.archive.core.FileSystemArchiveStore;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.CollectionMetadata;
import rosa.archive.model.ImageList;
import rosa.archive.model.Transcription;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Underline;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Preservation Property Tests.
 *
 * These tests capture EXISTING CORRECT BEHAVIOR on the unfixed code.
 * They MUST PASS on the current (unfixed) codebase, and continue to pass after fixes.
 *
 * <p><b>Validates: Requirements 2.5, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7</b>
 */
class PreservationPropertyTest {

    private IIIFPresentationGenerator generator;
    private IIIFJsonWriter writer;
    private ObjectMapper mapper;

    @BeforeEach
    @BeforeTry
    void setUp() {
        writer = new IIIFJsonWriter();
        generator = new IIIFPresentationGenerator(writer);
        mapper = writer.getObjectMapper();
    }

    // =========================================================================
    // Property: Standalone collections (no parents, no children) appear as
    // independent top-level items in the top-level collection.json
    // Validates: Requirements 3.1, 3.7
    // =========================================================================

    /**
     * For all standalone collections (no parents, no children), they appear as
     * independent top-level items in the top-level collection.json.
     *
     * <p><b>Validates: Requirements 3.1, 3.7</b>
     */
    @Test
    void standaloneCollection_appearsAsTopLevelItem() throws IOException {
        // "aor" is a standalone collection — no parents, no children
        List<String> collectionIds = List.of("aor", "dlmm", "rose");

        ObjectNode topCollection = generator.generateTopCollection(collectionIds, "http://example.org");
        String json = writer.writeToString(topCollection);
        JsonNode parsed = mapper.readTree(json);

        JsonNode items = parsed.get("items");
        assertNotNull(items);

        // aor should be present as a top-level item (current behavior for standalone)
        boolean found = false;
        for (JsonNode item : items) {
            String id = item.get("id").asText();
            if (id.contains("aor")) {
                found = true;
                assertEquals("Collection", item.get("type").asText());
                break;
            }
        }
        assertTrue(found, "Standalone collection 'aor' should appear as a top-level item");
    }

    /**
     * Property test: for ANY set of standalone collection IDs, every one of them
     * appears as a top-level item in generateTopCollection().
     *
     * <p><b>Validates: Requirements 3.1, 3.7</b>
     */
    @Property(tries = 50)
    void allStandaloneCollections_appearInTopLevelCollection(
            @ForAll("standaloneCollectionIds") List<String> collectionIds) throws IOException {

        ObjectNode topCollection = generator.generateTopCollection(collectionIds, "http://example.org");
        String json = writer.writeToString(topCollection);
        JsonNode parsed = mapper.readTree(json);

        JsonNode items = parsed.get("items");
        assertNotNull(items, "Top collection should have items");
        assertEquals(collectionIds.size(), items.size(),
                "All standalone collections should appear in top collection");

        for (int i = 0; i < collectionIds.size(); i++) {
            String expectedId = "http://example.org/" + collectionIds.get(i) + "/collection";
            assertEquals(expectedId, items.get(i).get("id").asText());
            assertEquals("Collection", items.get(i).get("type").asText());
        }
    }

    @Provide
    Arbitrary<List<String>> standaloneCollectionIds() {
        Arbitrary<String> collId = Arbitraries.of("aor", "dlmm", "rose", "pizan", "testcol", "archive1");
        return collId.list().ofMinSize(1).ofMaxSize(5).uniqueElements();
    }

    // =========================================================================
    // Property: For all 3-digit folio numbers (100–999), extractPageTranscription()
    // correctly matches TEI <pb n="..."/> markers.
    // Validates: Requirements 2.5, 3.3
    // =========================================================================

    /**
     * Property test: for all 3-digit folio numbers (100–999),
     * extractPageTranscription() correctly matches TEI page break markers.
     * This works because 3-digit folios have no leading zeros to strip.
     *
     * <p><b>Validates: Requirements 2.5</b>
     */
    @Property(tries = 100)
    void threeDigitFolioNumbers_matchTeiPageBreaks(
            @ForAll @IntRange(min = 100, max = 999) int folioNum,
            @ForAll("rectoVerso") String rv) throws Exception {

        String folio = folioNum + rv;
        String imageId = "Book." + folio + ".tif";

        // Build TEI XML with this page break
        String teiXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<TEI><text><body>"
                + "<pb n=\"" + folio + "\"/>"
                + "<p>Content for folio " + folio + " goes here.</p>"
                + "<pb n=\"nextpage\"/>"
                + "</body></text></TEI>";

        Method method = IIIFPresentationGenerator.class.getDeclaredMethod(
                "extractPageTranscription", String.class, String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(generator, teiXml, imageId);

        assertNotNull(result,
                "extractPageTranscription() should match 3-digit folio '" + folio + "'");
        assertTrue(result.contains("Content for folio " + folio),
                "Result should contain the content for folio " + folio);
    }

    @Provide
    Arbitrary<String> rectoVerso() {
        return Arbitraries.of("r", "v");
    }

    // =========================================================================
    // Property: For all books without .imagetag.csv, getIllustrationTagging()
    // is null and no errors occur.
    // Validates: Requirements 3.2
    // =========================================================================

    /**
     * For all books without .imagetag.csv, getIllustrationTagging() is null
     * and no errors occur during loadBook().
     *
     * <p><b>Validates: Requirements 3.2</b>
     */
    @Test
    void bookWithoutImagetagCsv_hasNullIllustrationTagging(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path collectionDir = archiveDir.resolve("testcol");
        Path bookDir = collectionDir.resolve("TestBook");
        Files.createDirectories(bookDir);

        // Create minimal image list — no imagetag.csv file
        Files.writeString(bookDir.resolve("TestBook.images.csv"),
                "TestBook.001r.tif,3000,4000,false\n");

        FileSystemArchiveStore store = new FileSystemArchiveStore(archiveDir);
        BookCollection collection = store.loadCollection("testcol");
        Book book = store.loadBook(collection, "TestBook");

        // PRESERVATION: books without imagetag.csv should have null illustration tagging
        assertNull(book.getIllustrationTagging(),
                "Book without .imagetag.csv should have null illustrationTagging");
    }

    /**
     * Property: for any book ID that does NOT have an imagetag.csv file,
     * loadBook succeeds and returns null illustration tagging.
     *
     * <p><b>Validates: Requirements 3.2</b>
     */
    @Property(tries = 5)
    void anyBookWithoutImagetagCsv_loadsWithoutErrors(
            @ForAll("bookIds") String bookId) throws IOException {

        Path tempDir = Files.createTempDirectory("preservation-test");
        try {
            Path archiveDir = tempDir.resolve("archive");
            Path collectionDir = archiveDir.resolve("col");
            Path bookDir = collectionDir.resolve(bookId);
            Files.createDirectories(bookDir);

            Files.writeString(bookDir.resolve(bookId + ".images.csv"),
                    bookId + ".001r.tif,3000,4000,false\n");

            FileSystemArchiveStore store = new FileSystemArchiveStore(archiveDir);
            BookCollection collection = store.loadCollection("col");
            Book book = store.loadBook(collection, bookId);

            assertNull(book.getIllustrationTagging(),
                    "Book '" + bookId + "' without imagetag.csv should have null illustrationTagging");
            assertNotNull(book.getImages(),
                    "Book should still have images loaded");
        } finally {
            deleteRecursive(tempDir);
        }
    }

    @Provide
    Arbitrary<String> bookIds() {
        return Arbitraries.of("Douce195", "Walters143", "LudwigXV7", "TestBook1", "PrincetonU101");
    }

    // =========================================================================
    // Property: For all books with empty image lists, generation skips them
    // without errors.
    // Validates: Requirements 3.4
    // =========================================================================

    /**
     * Books with empty image lists are skipped without errors during generation.
     *
     * <p><b>Validates: Requirements 3.4</b>
     */
    @Test
    void bookWithEmptyImageList_isSkippedWithoutErrors(@TempDir Path tempDir) throws IOException {
        Path archiveDir = tempDir.resolve("archive");
        Path collectionDir = archiveDir.resolve("testcol");
        Path bookDir = collectionDir.resolve("EmptyBook");
        Files.createDirectories(bookDir);

        // Empty image list file (no images)
        Files.writeString(bookDir.resolve("EmptyBook.images.csv"), "");

        Path outputDir = tempDir.resolve("output");

        FileSystemArchiveStore store = new FileSystemArchiveStore(archiveDir);
        // Should not throw — books with empty images are skipped
        assertDoesNotThrow(() ->
                generator.generate(store, outputDir, "http://example.org", null, 2));

        // Verify no manifest was generated for the empty book
        Path manifestPath = outputDir.resolve("testcol").resolve("EmptyBook").resolve("manifest.json");
        assertFalse(Files.exists(manifestPath),
                "No manifest should be generated for a book with empty image list");

        // Sub-collection should exist and be valid JSON
        Path subCollPath = outputDir.resolve("testcol").resolve("collection.json");
        assertTrue(Files.exists(subCollPath));
        String subCollJson = Files.readString(subCollPath);
        JsonNode subColl = mapper.readTree(subCollJson);
        assertEquals("Collection", subColl.get("type").asText(),
                "Sub-collection should be type Collection");
    }

    /**
     * Books with null images produce null annotation page and no errors.
     *
     * <p><b>Validates: Requirements 3.4</b>
     */
    @Test
    void bookWithNullImages_annotationPageReturnsNull() {
        BookCollection collection = createTestCollection("test", "Test");
        Book book = new Book();
        book.setId("EmptyBook");
        // images is null by default

        BookImage image = new BookImage("EmptyBook.001r.tif", 100, 100, false);

        // This should not throw — should return null because there's no content
        ObjectNode result = generator.generateAnnotationPage(collection, book, image, 0, "http://example.org");
        assertNull(result, "Annotation page for book with no data should be null");
    }

    // =========================================================================
    // Property: For all AoR annotated pages, annotation output is structurally consistent.
    // Validates: Requirements 3.5
    // =========================================================================

    /**
     * AoR annotations produce structurally consistent annotation JSON.
     * Underline annotations produce items with correct type, motivation, and body structure.
     *
     * <p><b>Validates: Requirements 3.5</b>
     */
    @Test
    void aorAnnotations_produceConsistentStructure() throws IOException {
        BookCollection collection = createTestCollection("aor", "Archaeology of Reading");
        Book book = createBookWithAnnotations("testbook", "testbook.001r.tif",
                "some underlined text", "la");

        BookImage image = book.getImages().getImages().get(0);

        ObjectNode annotationPage = generator.generateAnnotationPage(collection, book, image, 0,
                "http://example.org");

        assertNotNull(annotationPage, "Annotation page should not be null when annotations exist");

        String json = writer.writeToString(annotationPage);
        JsonNode parsed = mapper.readTree(json);

        // Structural checks
        assertEquals("AnnotationPage", parsed.get("type").asText());
        assertTrue(parsed.has("items"), "Annotation page should have items");

        JsonNode items = parsed.get("items");
        assertTrue(items.size() > 0, "Should have at least one annotation");

        // Each annotation has correct structure
        for (JsonNode item : items) {
            assertEquals("Annotation", item.get("type").asText());
            assertEquals("commenting", item.get("motivation").asText());
            assertTrue(item.has("body"), "Annotation should have a body");
            assertTrue(item.has("target"), "Annotation should have a target");

            JsonNode body = item.get("body");
            assertEquals("TextualBody", body.get("type").asText());
            assertTrue(body.has("value"), "Body should have a value");
            assertTrue(body.has("format"), "Body should have a format");
            assertTrue(body.has("language"), "Body should have a language");
        }
    }

    /**
     * Property: for AoR annotated pages with various underline texts, annotation output
     * always has consistent structure.
     *
     * <p><b>Validates: Requirements 3.5</b>
     */
    @Property(tries = 25)
    void aorAnnotations_alwaysProduceValidStructure(
            @ForAll("annotationTexts") String text,
            @ForAll("languages") String lang) throws IOException {

        BookCollection collection = createTestCollection("aor", "AoR");
        Book book = createBookWithAnnotations("book1", "book1.001r.tif", text, lang);
        BookImage image = book.getImages().getImages().get(0);

        ObjectNode annotationPage = generator.generateAnnotationPage(collection, book, image, 0,
                "http://example.org");

        // Non-empty text should always produce an annotation
        assertNotNull(annotationPage, "Annotation page should not be null for non-empty text");
        String json = writer.writeToString(annotationPage);
        JsonNode parsed = mapper.readTree(json);

        assertEquals("AnnotationPage", parsed.get("type").asText());
        JsonNode items = parsed.get("items");
        assertTrue(items.size() > 0);

        JsonNode annotation = items.get(0);
        assertEquals("Annotation", annotation.get("type").asText());
        assertEquals("commenting", annotation.get("motivation").asText());
        assertEquals("TextualBody", annotation.get("body").get("type").asText());
    }

    @Provide
    Arbitrary<String> annotationTexts() {
        return Arbitraries.of("hello world", "Latin text here", "annotated passage",
                "multi word text example", "short");
    }

    @Provide
    Arbitrary<String> languages() {
        return Arbitraries.of("en", "la", "fr", "it", "el");
    }

    // =========================================================================
    // Property: Canvas painting annotations and image services have expected structure.
    // Validates: Requirements 3.6
    // =========================================================================

    /**
     * Canvas painting annotations have ImageService with proper id, type, profile.
     *
     * <p><b>Validates: Requirements 3.6</b>
     */
    @Test
    void canvasPaintingAnnotation_hasCorrectStructure() throws IOException {
        BookCollection collection = createTestCollection("rose", "Rose");
        Book book = createTestBookWithImage("LudwigXV7", "LudwigXV7.001r.tif", 3000, 4000);
        BookImage image = book.getImages().getImages().get(0);

        ObjectNode canvas = generator.generateCanvas(collection, book, image, 0,
                "http://example.org", "https://images.example.com", 2, false);

        String json = writer.writeToString(canvas);
        JsonNode parsed = mapper.readTree(json);

        // Canvas structure
        assertEquals("Canvas", parsed.get("type").asText());
        assertEquals(3000, parsed.get("width").asInt());
        assertEquals(4000, parsed.get("height").asInt());

        // Painting annotation page
        JsonNode items = parsed.get("items");
        assertEquals(1, items.size());
        JsonNode annoPage = items.get(0);
        assertEquals("AnnotationPage", annoPage.get("type").asText());

        // Painting annotation
        JsonNode annotations = annoPage.get("items");
        assertEquals(1, annotations.size());
        JsonNode painting = annotations.get(0);
        assertEquals("Annotation", painting.get("type").asText());
        assertEquals("painting", painting.get("motivation").asText());

        // Body with image and service
        JsonNode body = painting.get("body");
        assertEquals("Image", body.get("type").asText());
        assertEquals("image/jpeg", body.get("format").asText());
        assertTrue(body.get("id").asText().endsWith("/full/full/0/default.jpg"));

        // Service array
        JsonNode service = body.get("service");
        assertTrue(service.isArray());
        assertEquals(1, service.size());
        JsonNode svc = service.get(0);
        assertEquals("ImageService2", svc.get("type").asText());
        assertEquals("http://iiif.io/api/image/2/level2.json", svc.get("profile").asText());
        assertTrue(svc.get("id").asText().startsWith("https://images.example.com/"));
    }

    /**
     * Property: for any image dimensions and API version, canvas painting annotations
     * always maintain the expected structure.
     *
     * <p><b>Validates: Requirements 3.6</b>
     */
    @Property(tries = 30)
    void canvasPaintingAnnotation_structurePreservedForAllInputs(
            @ForAll @IntRange(min = 100, max = 5000) int width,
            @ForAll @IntRange(min = 100, max = 5000) int height,
            @ForAll("apiVersions") int apiVersion) throws IOException {

        BookCollection collection = createTestCollection("col", "Collection");
        Book book = createTestBookWithImage("book", "book.001r.tif", width, height);
        BookImage image = book.getImages().getImages().get(0);

        ObjectNode canvas = generator.generateCanvas(collection, book, image, 0,
                "http://example.org", null, apiVersion, false);

        String json = writer.writeToString(canvas);
        JsonNode parsed = mapper.readTree(json);

        // Structural invariants that must always hold
        assertEquals("Canvas", parsed.get("type").asText());
        assertEquals(width, parsed.get("width").asInt());
        assertEquals(height, parsed.get("height").asInt());

        JsonNode annoPage = parsed.get("items").get(0);
        assertEquals("AnnotationPage", annoPage.get("type").asText());

        JsonNode painting = annoPage.get("items").get(0);
        assertEquals("Annotation", painting.get("type").asText());
        assertEquals("painting", painting.get("motivation").asText());

        JsonNode body = painting.get("body");
        assertEquals("Image", body.get("type").asText());

        JsonNode svc = body.get("service").get(0);
        assertNotNull(svc.get("id"));
        assertNotNull(svc.get("type"));
        assertNotNull(svc.get("profile"));

        // API version determines service type
        String expectedType = apiVersion == 3 ? "ImageService3" : "ImageService2";
        assertEquals(expectedType, svc.get("type").asText());
    }

    @Provide
    Arbitrary<Integer> apiVersions() {
        return Arbitraries.of(2, 3);
    }

    // =========================================================================
    // Additional preservation test: TEI page matching with book prefix works
    // Validates: Requirements 2.5
    // =========================================================================

    /**
     * extractPageTranscription() correctly matches when the short name (after book prefix)
     * matches the page break marker directly (3-digit case).
     *
     * <p><b>Validates: Requirements 2.5</b>
     */
    @Test
    void extractPageTranscription_shortNameMatch_worksFor3DigitFolio() throws Exception {
        String teiXml = "<TEI><text><body>"
                + "<pb n=\"100r\"/>"
                + "<p>Content for folio 100r.</p>"
                + "<pb n=\"100v\"/>"
                + "</body></text></TEI>";

        String imageId = "Book.100r.tif";

        Method method = IIIFPresentationGenerator.class.getDeclaredMethod(
                "extractPageTranscription", String.class, String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(generator, teiXml, imageId);
        assertNotNull(result, "Should match via short name path for 3-digit folio");
        assertTrue(result.contains("Content for folio 100r"));
    }

    /**
     * extractPageTranscription() returns null when there's no matching page break.
     *
     * <p><b>Validates: Requirements 3.3</b>
     */
    @Test
    void extractPageTranscription_noMatch_returnsNull() throws Exception {
        String teiXml = "<TEI><text><body>"
                + "<pb n=\"50r\"/>"
                + "<p>Content for 50r.</p>"
                + "</body></text></TEI>";

        String imageId = "Book.999r.tif";

        Method method = IIIFPresentationGenerator.class.getDeclaredMethod(
                "extractPageTranscription", String.class, String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(generator, teiXml, imageId);
        assertNull(result, "Should return null when no matching page break exists");
    }

    // =========================================================================
    // Preservation: Book without transcription generates no transcription annotations
    // Validates: Requirements 3.3
    // =========================================================================

    /**
     * A book without transcription XML produces no transcription annotations.
     *
     * <p><b>Validates: Requirements 3.3</b>
     */
    @Test
    void bookWithoutTranscription_generatesNoTranscriptionAnnotations() {
        BookCollection collection = createTestCollection("rose", "Rose");
        Book book = createTestBookWithImage("TestBook", "TestBook.001r.tif", 3000, 4000);
        // No transcription set — book.getTranscription() is null

        BookImage image = book.getImages().getImages().get(0);

        ObjectNode annotationPage = generator.generateAnnotationPage(collection, book, image, 0,
                "http://example.org");

        // No AoR annotations, no illustration tagging, no transcription → null
        assertNull(annotationPage,
                "Annotation page should be null when book has no annotation content");
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    private BookCollection createTestCollection(String id, String label) {
        BookCollection collection = new BookCollection();
        collection.setId(id);
        CollectionMetadata metadata = new CollectionMetadata();
        metadata.setLabel(label);
        collection.setMetadata(metadata);
        return collection;
    }

    private Book createTestBookWithImage(String bookId, String imageId, int width, int height) {
        BookImage image = new BookImage(imageId, width, height, false);
        image.setName(imageId.replace(".tif", ""));

        ImageList imageList = new ImageList();
        imageList.setImages(List.of(image));

        Book book = new Book();
        book.setId(bookId);
        book.setImages(imageList);
        return book;
    }

    private Book createBookWithAnnotations(String bookId, String imageId, String underlineText, String lang) {
        BookImage image = new BookImage(imageId, 3000, 4000, false);
        image.setName(imageId.replace(".tif", ""));

        ImageList imageList = new ImageList();
        imageList.setImages(List.of(image));

        Book book = new Book();
        book.setId(bookId);
        book.setImages(imageList);

        // Create an annotated page with an underline annotation
        AnnotatedPage ap = new AnnotatedPage();
        ap.setPage(imageId.replace(".tif", ""));

        Underline underline = new Underline();
        underline.setReferencedText(underlineText);
        underline.setLanguage(lang);
        ap.getUnderlines().add(underline);

        book.getAnnotatedPages().add(ap);
        return book;
    }

    private void deleteRecursive(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var stream = Files.list(path)) {
                for (Path child : stream.toList()) {
                    deleteRecursive(child);
                }
            }
        }
        Files.deleteIfExists(path);
    }
}
