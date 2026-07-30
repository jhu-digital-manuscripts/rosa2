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
import rosa.archive.model.BookMetadata;
import rosa.archive.model.CollectionMetadata;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
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
        List<String> bookIds = List.of("LudwigXV7", "Walters143");

        ObjectNode result = generator.generateSubCollection(collection, bookIds, "http://example.org");

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

        List<String> bookIds = List.of("Castiglione");

        ObjectNode result = generator.generateSubCollection(collection, bookIds, null);

        String json = writer.writeToString(result);
        JsonNode parsed = mapper.readTree(json);

        // Should fall back to collection id
        assertEquals("aor", parsed.get("label").get("none").get(0).asText());
    }

    @Test
    void testGenerateSubCollectionWithRelativeIds() throws IOException {
        BookCollection collection = createTestCollection("dlmm", "Digital Library of Medieval Manuscripts");
        List<String> bookIds = List.of("MS100");

        ObjectNode result = generator.generateSubCollection(collection, bookIds, null);

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
        generator.generate(store, outputDir, "http://example.org", 2);

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
        List<String> bookIds = List.of("LudwigXV7");

        ObjectNode result = generator.generateSubCollection(collection, bookIds, "http://example.org");

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

        List<String> bookIds = List.of("LudwigXV7");

        ObjectNode result = generator.generateSubCollection(collection, bookIds, "http://example.org");

        String json = writer.writeToString(result);
        JsonNode parsed = mapper.readTree(json);

        // Should use "fr" language key from collection metadata
        assertTrue(parsed.get("label").has("fr"));
        assertEquals("Roman de la Rose", parsed.get("label").get("fr").get(0).asText());
    }

    // --- Helper methods ---

    private BookCollection createTestCollection(String id, String label) {
        BookCollection collection = new BookCollection();
        collection.setId(id);
        CollectionMetadata metadata = new CollectionMetadata();
        metadata.setLabel(label);
        collection.setMetadata(metadata);
        return collection;
    }
}
