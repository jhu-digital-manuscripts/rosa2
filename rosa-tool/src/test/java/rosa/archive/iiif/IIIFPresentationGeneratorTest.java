package rosa.archive.iiif;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import rosa.archive.core.FileSystemArchiveStore;
import rosa.archive.model.BiblioData;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.CollectionMetadata;
import rosa.archive.model.ImageList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link IIIFPresentationGenerator} covering top-level and sub-collection generation.
 */
class IIIFPresentationGeneratorTest {

    private IIIFPresentationGenerator generator;
    private IIIFJsonWriter writer;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        writer = new IIIFJsonWriter();
        generator = new IIIFPresentationGenerator(writer);
        mapper = new ObjectMapper();
    }

    @Test
    void testGenerateTopCollectionWithBaseUrl() throws IOException {
        List<String> collectionIds = List.of("rose", "pizan", "aor");
        ObjectNode result = generator.generateTopCollection(collectionIds, "http://example.org");

        String json = writer.writeToString(result);
        JsonNode parsed = mapper.readTree(json);

        assertEquals("http://iiif.io/api/presentation/3/context.json", parsed.get("@context").asText());
        assertEquals("http://example.org/collection", parsed.get("id").asText());
        assertEquals("Collection", parsed.get("type").asText());

        // Label should use "none" key with "rosa2 Archive"
        assertTrue(parsed.has("label"));
        JsonNode label = parsed.get("label");
        assertTrue(label.has("none"));
        assertEquals("rosa2 Archive", label.get("none").get(0).asText());

        // Items array
        assertTrue(parsed.has("items"));
        JsonNode items = parsed.get("items");
        assertEquals(3, items.size());

        assertEquals("http://example.org/rose/collection", items.get(0).get("id").asText());
        assertEquals("Collection", items.get(0).get("type").asText());
        assertEquals("rose", items.get(0).get("label").get("none").get(0).asText());

        assertEquals("http://example.org/pizan/collection", items.get(1).get("id").asText());
        assertEquals("http://example.org/aor/collection", items.get(2).get("id").asText());
    }

    @Test
    void testGenerateTopCollectionWithoutBaseUrl() throws IOException {
        List<String> collectionIds = List.of("rose");
        ObjectNode result = generator.generateTopCollection(collectionIds, null);

        String json = writer.writeToString(result);
        JsonNode parsed = mapper.readTree(json);

        assertEquals("collection", parsed.get("id").asText());
        assertEquals("rose/collection", parsed.get("items").get(0).get("id").asText());
    }

    @Test
    void testGenerateTopCollectionEmptyBaseUrl() throws IOException {
        List<String> collectionIds = List.of("dlmm");
        ObjectNode result = generator.generateTopCollection(collectionIds, "");

        String json = writer.writeToString(result);
        JsonNode parsed = mapper.readTree(json);

        // Empty baseUrl should behave like null (relative paths)
        assertEquals("collection", parsed.get("id").asText());
    }

    @Test
    void testGenerateSubCollectionWithBaseUrl() throws IOException {
        BookCollection collection = createTestCollection("rose", "Roman de la Rose Digital Library");
        Map<String, String> bookLabels = new LinkedHashMap<>();
        bookLabels.put("LudwigXV7", "LudwigXV7");
        bookLabels.put("Walters143", "Walters143");

        ObjectNode result = generator.generateSubCollection(collection, bookLabels, "http://example.org");

        String json = writer.writeToString(result);
        JsonNode parsed = mapper.readTree(json);

        assertEquals("http://iiif.io/api/presentation/3/context.json", parsed.get("@context").asText());
        assertEquals("http://example.org/rose/collection", parsed.get("id").asText());
        assertEquals("Collection", parsed.get("type").asText());

        JsonNode items = parsed.get("items");
        assertEquals(2, items.size());

        assertEquals("http://example.org/rose/LudwigXV7/manifest", items.get(0).get("id").asText());
        assertEquals("Manifest", items.get(0).get("type").asText());

        assertEquals("http://example.org/rose/Walters143/manifest", items.get(1).get("id").asText());
    }

    @Test
    void testGenerateSubCollectionWithoutLabel() throws IOException {
        BookCollection collection = new BookCollection();
        collection.setId("aor");
        // No metadata set, so getLabel() returns null

        Map<String, String> bookLabels = new LinkedHashMap<>();
        bookLabels.put("Castiglione", "Castiglione");

        ObjectNode result = generator.generateSubCollection(collection, bookLabels, null);

        String json = writer.writeToString(result);
        JsonNode parsed = mapper.readTree(json);

        // Should fall back to collection id
        assertEquals("aor", parsed.get("label").get("none").get(0).asText());
    }

    @Test
    void testGenerateSubCollectionWithRelativeIds() throws IOException {
        BookCollection collection = createTestCollection("dlmm", "Digital Library of Medieval Manuscripts");
        Map<String, String> bookLabels = new LinkedHashMap<>();
        bookLabels.put("MS100", "MS100");

        ObjectNode result = generator.generateSubCollection(collection, bookLabels, null);

        String json = writer.writeToString(result);
        JsonNode parsed = mapper.readTree(json);

        assertEquals("dlmm/collection", parsed.get("id").asText());
        assertEquals("dlmm/MS100/manifest", parsed.get("items").get(0).get("id").asText());
    }

    @Test
    void testGenerateUsesIiif3Properties() throws IOException {
        List<String> collectionIds = List.of("test");
        ObjectNode result = generator.generateTopCollection(collectionIds, "http://example.org");

        String json = writer.writeToString(result);

        // IIIF 3.0: uses "type" not "@type", "id" not "@id"
        assertFalse(json.contains("\"@type\""));
        assertFalse(json.contains("\"@id\""));
        assertTrue(json.contains("\"type\""));
        assertTrue(json.contains("\"id\""));
        // Uses "items" not "sequences" or "members"
        assertTrue(json.contains("\"items\""));
        assertFalse(json.contains("\"sequences\""));
        assertFalse(json.contains("\"members\""));
    }

    @Test
    void testGenerateWritesFilesWithFileSystemStore(@TempDir Path tempDir) throws IOException {
        // Create a minimal test archive structure
        Path archiveDir = tempDir.resolve("archive");
        Path collectionDir = archiveDir.resolve("testcollection");
        Path bookDir = collectionDir.resolve("testbook");
        Files.createDirectories(bookDir);

        // Create minimal image list file (tab-separated: id width height missing)
        Files.writeString(bookDir.resolve("testbook.images.csv"),
                "*testbook.001r.tif\t3000\t4000\t0\n");

        Path outputDir = tempDir.resolve("output");

        FileSystemArchiveStore store = new FileSystemArchiveStore(archiveDir);
        generator.generate(store, outputDir, "http://example.org", null, 2);

        // Top-level collection.json should exist
        Path topCollection = outputDir.resolve("collection.json");
        assertTrue(Files.exists(topCollection));

        String topJson = Files.readString(topCollection);
        JsonNode topParsed = mapper.readTree(topJson);
        assertEquals("Collection", topParsed.get("type").asText());
        assertEquals("http://example.org/collection", topParsed.get("id").asText());

        // Per-collection file should exist
        Path subCollection = outputDir.resolve("testcollection").resolve("collection.json");
        assertTrue(Files.exists(subCollection));

        String subJson = Files.readString(subCollection);
        JsonNode subParsed = mapper.readTree(subJson);
        assertEquals("Collection", subParsed.get("type").asText());

        // Sub-collection should reference the manifest with proper label (falls back to bookId)
        JsonNode subItems = subParsed.get("items");
        assertEquals(1, subItems.size());
        assertEquals("http://example.org/testcollection/testbook/manifest", subItems.get(0).get("id").asText());

        // Manifest should exist
        Path manifest = outputDir.resolve("testcollection").resolve("testbook").resolve("manifest.json");
        assertTrue(Files.exists(manifest));
    }

    @Test
    void testBaseUrlTrailingSlash() throws IOException {
        List<String> collectionIds = List.of("rose");
        ObjectNode result = generator.generateTopCollection(collectionIds, "http://example.org/");

        String json = writer.writeToString(result);
        JsonNode parsed = mapper.readTree(json);

        // Should not have double slashes
        assertEquals("http://example.org/collection", parsed.get("id").asText());
        assertEquals("http://example.org/rose/collection", parsed.get("items").get(0).get("id").asText());
    }

    @Test
    void testOutputMatchesExpectedFormat() throws IOException {
        List<String> collectionIds = List.of("rose");
        ObjectNode result = generator.generateTopCollection(collectionIds, "http://example.org");

        String json = writer.writeToString(result);

        // Should match expected format from the task description with sorted keys
        String expected = "{\"@context\":\"http://iiif.io/api/presentation/3/context.json\"," +
                "\"id\":\"http://example.org/collection\"," +
                "\"items\":[{\"id\":\"http://example.org/rose/collection\"," +
                "\"label\":{\"none\":[\"rose\"]},\"type\":\"Collection\"}]," +
                "\"label\":{\"none\":[\"rosa2 Archive\"]},\"type\":\"Collection\"}";
        assertEquals(expected, json);
    }

    @Test
    void testSubCollectionLabelUsesCollectionMetadataLabel() throws IOException {
        BookCollection collection = createTestCollection("rose", "Roman de la Rose");
        Map<String, String> bookLabels = new LinkedHashMap<>();
        bookLabels.put("LudwigXV7", "LudwigXV7");

        ObjectNode result = generator.generateSubCollection(collection, bookLabels, "http://example.org");

        String json = writer.writeToString(result);
        JsonNode parsed = mapper.readTree(json);

        // Collection label from metadata
        assertEquals("Roman de la Rose", parsed.get("label").get("none").get(0).asText());
    }

    @Test
    void testSubCollectionLabelUsesLanguageKey() throws IOException {
        // Collection with a supported language
        BookCollection collection = new BookCollection();
        collection.setId("rose");
        CollectionMetadata metadata = new CollectionMetadata();
        metadata.setLabel("Roman de la Rose");
        metadata.setLanguages(new String[]{"fr"});
        collection.setMetadata(metadata);

        Map<String, String> bookLabels = new LinkedHashMap<>();
        bookLabels.put("LudwigXV7", "LudwigXV7");

        ObjectNode result = generator.generateSubCollection(collection, bookLabels, "http://example.org");

        String json = writer.writeToString(result);
        JsonNode parsed = mapper.readTree(json);

        // Should use "fr" language key from collection metadata
        assertTrue(parsed.get("label").has("fr"));
        assertEquals("Roman de la Rose", parsed.get("label").get("fr").get(0).asText());
    }

    @Test
    void testCanvasImageServiceUsesImageBaseUrl() throws IOException {
        BookCollection collection = createTestCollection("rose", "Roman de la Rose");
        Book book = createTestBookWithImage("LudwigXV7", "LudwigXV7.001r.tif", 3000, 4000);

        BookImage image = book.getImages().getImages().get(0);
        String baseUrl = "http://example.org";
        String imageBaseUrl = "https://images.example.com";

        ObjectNode canvas = generator.generateCanvas(collection, book, image, 0, baseUrl, imageBaseUrl, 2, false);

        String json = writer.writeToString(canvas);
        JsonNode parsed = mapper.readTree(json);

        // Canvas ID should use baseUrl, not imageBaseUrl
        assertEquals("http://example.org/rose/LudwigXV7/canvas/0", parsed.get("id").asText());

        // Image Service ID should use imageBaseUrl with encoded slashes and no file extension
        JsonNode service = parsed.get("items").get(0).get("items").get(0).get("body").get("service").get(0);
        assertEquals("https://images.example.com/rose%2FLudwigXV7%2FLudwigXV7.001r", service.get("id").asText());
    }

    @Test
    void testCanvasImageServiceFallsBackToBaseUrl() throws IOException {
        BookCollection collection = createTestCollection("rose", "Roman de la Rose");
        Book book = createTestBookWithImage("LudwigXV7", "LudwigXV7.001r.tif", 3000, 4000);

        BookImage image = book.getImages().getImages().get(0);
        String baseUrl = "http://example.org";

        ObjectNode canvas = generator.generateCanvas(collection, book, image, 0, baseUrl, null, 2, false);

        String json = writer.writeToString(canvas);
        JsonNode parsed = mapper.readTree(json);

        // Canvas ID should use baseUrl
        assertEquals("http://example.org/rose/LudwigXV7/canvas/0", parsed.get("id").asText());

        // Image Service ID should fall back to baseUrl when imageBaseUrl is null, with encoded slashes and no extension
        JsonNode service = parsed.get("items").get(0).get("items").get(0).get("body").get("service").get(0);
        assertEquals("http://example.org/rose%2FLudwigXV7%2FLudwigXV7.001r", service.get("id").asText());
    }

    @Test
    void testManifestThumbnailUsesImageBaseUrl() throws IOException {
        BookCollection collection = createTestCollection("rose", "Roman de la Rose");
        Book book = createTestBookWithImage("LudwigXV7", "LudwigXV7.001r.tif", 3000, 4000);

        String baseUrl = "http://example.org";
        String imageBaseUrl = "https://images.example.com";

        ObjectNode manifest = generator.generateManifest(collection, book, baseUrl, imageBaseUrl, 2);

        String json = writer.writeToString(manifest);
        JsonNode parsed = mapper.readTree(json);

        // Manifest ID should use baseUrl
        assertEquals("http://example.org/rose/LudwigXV7/manifest", parsed.get("id").asText());

        // Thumbnail should use imageBaseUrl with encoded image identifier
        String thumbnailId = parsed.get("thumbnail").get(0).get("id").asText();
        assertTrue(thumbnailId.startsWith("https://images.example.com/"),
                "Thumbnail should use imageBaseUrl, got: " + thumbnailId);
        assertEquals("https://images.example.com/rose%2FLudwigXV7%2FLudwigXV7.001r/full/80,/0/default.jpg",
                thumbnailId);
    }

    // --- Tests for collection item labels (Requirement 3.4) ---

    @Test
    void testSubCollectionItemsUseProvidedLabels() throws IOException {
        BookCollection collection = createTestCollection("rose", "Roman de la Rose Digital Library");
        Map<String, String> bookLabels = new LinkedHashMap<>();
        bookLabels.put("LudwigXV7", "Ludwig XV 7 (Getty)");
        bookLabels.put("Walters143", "Walters Art Museum MS 143");

        ObjectNode result = generator.generateSubCollection(collection, bookLabels, "http://example.org");

        String json = writer.writeToString(result);
        JsonNode parsed = mapper.readTree(json);

        JsonNode items = parsed.get("items");
        assertEquals(2, items.size());

        // Verify labels use the provided commonName values, not the bookIds
        assertEquals("Ludwig XV 7 (Getty)", items.get(0).get("label").get("none").get(0).asText());
        assertEquals("Walters Art Museum MS 143", items.get(1).get("label").get("none").get(0).asText());

        // Verify IDs still use the bookId
        assertEquals("http://example.org/rose/LudwigXV7/manifest", items.get(0).get("id").asText());
        assertEquals("http://example.org/rose/Walters143/manifest", items.get(1).get("id").asText());
    }

    @Test
    void testSubCollectionItemsFallbackToBookId() throws IOException {
        BookCollection collection = createTestCollection("aor", "Archaeology of Reading");
        Map<String, String> bookLabels = new LinkedHashMap<>();
        // When no BiblioData is available, the label in the map IS the bookId
        bookLabels.put("BL531k6", "BL531k6");
        bookLabels.put("PrincetonPA6452", "PrincetonPA6452");

        ObjectNode result = generator.generateSubCollection(collection, bookLabels, "http://example.org");

        String json = writer.writeToString(result);
        JsonNode parsed = mapper.readTree(json);

        JsonNode items = parsed.get("items");
        assertEquals(2, items.size());

        // When no BiblioData is available, the bookId is used as the label
        assertEquals("BL531k6", items.get(0).get("label").get("none").get(0).asText());
        assertEquals("PrincetonPA6452", items.get(1).get("label").get("none").get(0).asText());

        // IDs still use the bookId
        assertEquals("http://example.org/aor/BL531k6/manifest", items.get(0).get("id").asText());
        assertEquals("http://example.org/aor/PrincetonPA6452/manifest", items.get(1).get("id").asText());
    }

    @Test
    void testGenerateWithBiblioDataUsesCommonNameInSubCollection(@TempDir Path tempDir) throws IOException {
        // Create a minimal archive structure with BiblioData containing a commonName
        Path archiveDir = tempDir.resolve("archive");
        Path collectionDir = archiveDir.resolve("testcol");
        Path bookDir = collectionDir.resolve("TestBook1");
        Files.createDirectories(bookDir);

        // Create image list (required so the book isn't skipped)
        Files.writeString(bookDir.resolve("TestBook1.images.csv"),
                "*TestBook1.001r.tif\t3000\t4000\t0\n");

        // Create metadata XML with BiblioData containing commonName
        String metadataXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <book>
                  <bibliographies>
                    <bibliography lang="en">
                      <commonName>A Beautiful Manuscript</commonName>
                      <title>Test Book One: Full Title</title>
                      <repository>Test Library</repository>
                    </bibliography>
                  </bibliographies>
                </book>
                """;
        Files.writeString(bookDir.resolve("TestBook1.metadata.xml"), metadataXml);

        Path outputDir = tempDir.resolve("output");

        FileSystemArchiveStore store = new FileSystemArchiveStore(archiveDir);
        generator.generate(store, outputDir, "http://example.org", null, 2);

        // Read the sub-collection JSON
        Path subCollectionFile = outputDir.resolve("testcol").resolve("collection.json");
        assertTrue(Files.exists(subCollectionFile), "Sub-collection file should exist");

        String subJson = Files.readString(subCollectionFile);
        JsonNode subParsed = mapper.readTree(subJson);

        JsonNode items = subParsed.get("items");
        assertNotNull(items, "Sub-collection should have items");
        assertEquals(1, items.size());

        // The label should be the commonName from BiblioData, not the bookId
        String label = items.get(0).get("label").get("none").get(0).asText();
        assertEquals("A Beautiful Manuscript", label,
                "Sub-collection item label should use BiblioData commonName");
    }

    @Test
    void testAnnotationPageWrittenToFile(@TempDir Path tempDir) throws IOException {
        // Create an archive with a book that has an AoR transcription XML
        Path archiveDir = tempDir.resolve("archive");
        Path collectionDir = archiveDir.resolve("aor");
        Path bookDir = collectionDir.resolve("testbook");
        Files.createDirectories(bookDir);

        // Create image list (comma-separated)
        Files.writeString(bookDir.resolve("testbook.images.csv"),
                "testbook.001r.tif,3000,4000,Title Page\n" +
                "testbook.002r.tif,3000,4000,2\n");

        // Create AoR transcription XML for first page
        String aorXml = """
                <?xml version="1.0" encoding="UTF-8" standalone="no"?>
                <transcription xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:noNamespaceSchemaLocation="http://www.livesandletters.ac.uk/schema/aor2_18112016.xsd">
                    <page filename="testbook.001r.tif" pagination="Title Page" reader="Harvey"/>
                    <annotation>
                        <underline id="u1" method="pen" type="straight" language="la" text="example underlined text"/>
                    </annotation>
                </transcription>
                """;
        Files.writeString(bookDir.resolve("testbook.aor.001r.xml"), aorXml);

        Path outputDir = tempDir.resolve("output");

        FileSystemArchiveStore store = new FileSystemArchiveStore(archiveDir);
        generator.generate(store, outputDir, "http://example.org", null, 2);

        // Verify annotation page file was written to canvas/0/annotations.json
        Path annotationFile = outputDir.resolve("aor").resolve("testbook")
                .resolve("canvas").resolve("0").resolve("annotations.json");
        assertTrue(Files.exists(annotationFile),
                "Annotation page file should exist at canvas/0/annotations.json");
    }

    @Test
    void testAnnotationPageFileIncludesContext(@TempDir Path tempDir) throws IOException {
        // Create an archive with a book that has an AoR transcription XML
        Path archiveDir = tempDir.resolve("archive");
        Path collectionDir = archiveDir.resolve("aor");
        Path bookDir = collectionDir.resolve("testbook");
        Files.createDirectories(bookDir);

        Files.writeString(bookDir.resolve("testbook.images.csv"),
                "testbook.001r.tif,3000,4000,Title Page\n");

        String aorXml = """
                <?xml version="1.0" encoding="UTF-8" standalone="no"?>
                <transcription xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:noNamespaceSchemaLocation="http://www.livesandletters.ac.uk/schema/aor2_18112016.xsd">
                    <page filename="testbook.001r.tif" pagination="Title Page" reader="Harvey"/>
                    <annotation>
                        <underline id="u1" method="pen" type="straight" language="la" text="some latin text"/>
                    </annotation>
                </transcription>
                """;
        Files.writeString(bookDir.resolve("testbook.aor.001r.xml"), aorXml);

        Path outputDir = tempDir.resolve("output");

        FileSystemArchiveStore store = new FileSystemArchiveStore(archiveDir);
        generator.generate(store, outputDir, "http://example.org", null, 2);

        // Read the annotations.json file and verify @context
        Path annotationFile = outputDir.resolve("aor").resolve("testbook")
                .resolve("canvas").resolve("0").resolve("annotations.json");
        assertTrue(Files.exists(annotationFile));

        String json = Files.readString(annotationFile);
        JsonNode parsed = mapper.readTree(json);

        assertEquals("http://iiif.io/api/presentation/3/context.json",
                parsed.get("@context").asText(),
                "Annotation page file should include @context");
        assertEquals("AnnotationPage", parsed.get("type").asText());
        assertTrue(parsed.has("items"), "Annotation page should have items array");
    }

    @Test
    void testCanvasAnnotationsPropertyIsReference() throws IOException {
        BookCollection collection = createTestCollection("aor", "Archaeology of Reading");
        Book book = createTestBookWithImage("testbook", "testbook.001r.tif", 3000, 4000);

        BookImage image = book.getImages().getImages().get(0);

        // Generate canvas with hasAnnotations=true
        ObjectNode canvas = generator.generateCanvas(collection, book, image, 0,
                "http://example.org", null, 2, true);

        String json = writer.writeToString(canvas);
        JsonNode parsed = mapper.readTree(json);

        // Verify annotations property exists and is an array
        assertTrue(parsed.has("annotations"), "Canvas should have annotations property");
        JsonNode annotations = parsed.get("annotations");
        assertTrue(annotations.isArray());
        assertEquals(1, annotations.size());

        // Verify the reference contains only id and type (no items, no full content)
        JsonNode ref = annotations.get(0);
        assertEquals("http://example.org/aor/testbook/canvas/0/annotations.json", ref.get("id").asText());
        assertEquals("AnnotationPage", ref.get("type").asText());
        assertFalse(ref.has("items"), "Annotation reference should not contain items (full content)");
    }

    @Test
    void testCanvasWithNoAnnotationsOmitsProperty() throws IOException {
        BookCollection collection = createTestCollection("aor", "Archaeology of Reading");
        Book book = createTestBookWithImage("testbook", "testbook.001r.tif", 3000, 4000);

        BookImage image = book.getImages().getImages().get(0);

        // Generate canvas with hasAnnotations=false
        ObjectNode canvas = generator.generateCanvas(collection, book, image, 0,
                "http://example.org", null, 2, false);

        String json = writer.writeToString(canvas);
        JsonNode parsed = mapper.readTree(json);

        // Verify there is NO annotations property
        assertFalse(parsed.has("annotations"),
                "Canvas with no annotation content should omit the annotations property");
    }

    // --- Tests for missing image handling ---

    @Test
    void testCanvasMissingImageReferencesMissingImagePlaceholder() throws IOException {
        BookCollection collection = createTestCollection("rose", "Roman de la Rose");
        // Set the collection's missing image placeholder
        BookImage missingImage = new BookImage("missing_image.tif", 100, 100, false);
        collection.setMissingImage(missingImage);

        // Create a book with a missing image
        BookImage image = new BookImage("LudwigXV7.001r.tif", 3000, 4000, true);
        image.setName("LudwigXV7.001r");
        ImageList imageList = new ImageList();
        imageList.setImages(List.of(image));
        Book book = new Book();
        book.setId("LudwigXV7");
        book.setImages(imageList);

        ObjectNode canvas = generator.generateCanvas(collection, book, image, 0,
                "http://example.org", "https://images.example.com", 2, false);

        String json = writer.writeToString(canvas);
        JsonNode parsed = mapper.readTree(json);

        // Image service ID should reference missing_image in the collection (no book in path)
        JsonNode service = parsed.get("items").get(0).get("items").get(0).get("body").get("service").get(0);
        assertEquals("https://images.example.com/rose%2Fmissing_image", service.get("id").asText());
    }

    @Test
    void testCanvasMissingImageFallsBackToDefaultName() throws IOException {
        BookCollection collection = createTestCollection("rose", "Roman de la Rose");
        // No missing image set on the collection

        BookImage image = new BookImage("LudwigXV7.001r.tif", 3000, 4000, true);
        image.setName("LudwigXV7.001r");
        ImageList imageList = new ImageList();
        imageList.setImages(List.of(image));
        Book book = new Book();
        book.setId("LudwigXV7");
        book.setImages(imageList);

        ObjectNode canvas = generator.generateCanvas(collection, book, image, 0,
                "http://example.org", "https://images.example.com", 2, false);

        String json = writer.writeToString(canvas);
        JsonNode parsed = mapper.readTree(json);

        // Should fallback to "missing_image.tif" (with extension stripped)
        JsonNode service = parsed.get("items").get(0).get("items").get(0).get("body").get("service").get(0);
        assertEquals("https://images.example.com/rose%2Fmissing_image", service.get("id").asText());
    }

    // --- Tests for cropped image support ---

    @Test
    void testCanvasWithCroppedFlagUsesCroppedPath() throws IOException {
        BookCollection collection = createTestCollection("rose", "Roman de la Rose");
        Book book = createTestBookWithImage("LudwigXV7", "LudwigXV7.001r.tif", 3000, 4000);

        BookImage image = book.getImages().getImages().get(0);

        // Generate canvas with cropped=true
        ObjectNode canvas = generator.generateCanvas(collection, book, image, 0,
                "http://example.org", "https://images.example.com", 2, false, true);

        String json = writer.writeToString(canvas);
        JsonNode parsed = mapper.readTree(json);

        // Image service ID should include "cropped" in the path
        JsonNode service = parsed.get("items").get(0).get("items").get(0).get("body").get("service").get(0);
        assertEquals("https://images.example.com/rose%2FLudwigXV7%2Fcropped%2FLudwigXV7.001r", service.get("id").asText());
    }

    @Test
    void testManifestUsesCroppedImagesWhenAvailable() throws IOException {
        BookCollection collection = createTestCollection("rose", "Roman de la Rose");

        // Create a book with regular images and cropped images
        BookImage regularImage = new BookImage("LudwigXV7.001r.tif", 3000, 4000, false);
        regularImage.setName("LudwigXV7.001r");
        ImageList regularList = new ImageList();
        regularList.setImages(List.of(regularImage));

        BookImage croppedImage = new BookImage("LudwigXV7.001r.tif", 2800, 3800, false);
        croppedImage.setName("LudwigXV7.001r");
        ImageList croppedList = new ImageList();
        croppedList.setImages(List.of(croppedImage));

        Book book = new Book();
        book.setId("LudwigXV7");
        book.setImages(regularList);
        book.setCroppedImages(croppedList);

        ObjectNode manifest = generator.generateManifest(collection, book,
                "http://example.org", "https://images.example.com", 2);

        String json = writer.writeToString(manifest);
        JsonNode parsed = mapper.readTree(json);

        // Canvas image service should use the cropped path
        JsonNode canvasService = parsed.get("items").get(0).get("items").get(0).get("items").get(0)
                .get("body").get("service").get(0);
        assertEquals("https://images.example.com/rose%2FLudwigXV7%2Fcropped%2FLudwigXV7.001r",
                canvasService.get("id").asText());

        // Manifest thumbnail should also use the cropped path
        String thumbnailId = parsed.get("thumbnail").get(0).get("id").asText();
        assertTrue(thumbnailId.contains("cropped"), "Thumbnail should use cropped path");
    }

    @Test
    void testManifestFallsBackToRegularImagesWhenNoCropped() throws IOException {
        BookCollection collection = createTestCollection("rose", "Roman de la Rose");
        Book book = createTestBookWithImage("LudwigXV7", "LudwigXV7.001r.tif", 3000, 4000);
        // No cropped images set

        ObjectNode manifest = generator.generateManifest(collection, book,
                "http://example.org", "https://images.example.com", 2);

        String json = writer.writeToString(manifest);
        JsonNode parsed = mapper.readTree(json);

        // Canvas image service should NOT have "cropped" in the path
        JsonNode canvasService = parsed.get("items").get(0).get("items").get(0).get("items").get(0)
                .get("body").get("service").get(0);
        assertEquals("https://images.example.com/rose%2FLudwigXV7%2FLudwigXV7.001r",
                canvasService.get("id").asText());
    }

    // --- Helper methods ---

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

    private BookCollection createTestCollection(String id, String label) {
        BookCollection collection = new BookCollection();
        collection.setId(id);
        CollectionMetadata metadata = new CollectionMetadata();
        metadata.setLabel(label);
        collection.setMetadata(metadata);
        return collection;
    }
}
