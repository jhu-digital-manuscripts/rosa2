package rosa.archive.iiif;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class IIIFJsonWriterTest {

    private IIIFJsonWriter writer;

    @BeforeEach
    void setUp() {
        writer = new IIIFJsonWriter();
    }

    @Test
    void writeProducesCompactSingleLineJson(@TempDir Path tempDir) throws IOException {
        ObjectNode resource = writer.createObjectNode();
        resource.put("type", "Manifest");
        resource.put("id", "http://example.org/manifest/1");

        Path outputFile = tempDir.resolve("manifest.json");
        writer.write(resource, outputFile);

        String content = Files.readString(outputFile, StandardCharsets.UTF_8);
        assertFalse(content.contains("\n") && content.indexOf("\n") < content.length() - 1,
                "Output should be compact single-line JSON (no internal newlines)");
    }

    @Test
    void writeUsesUtf8Encoding(@TempDir Path tempDir) throws IOException {
        ObjectNode resource = writer.createObjectNode();
        resource.put("type", "Manifest");
        resource.put("label", "\u00e9\u00e8\u00ea\u00eb");

        Path outputFile = tempDir.resolve("manifest.json");
        writer.write(resource, outputFile);

        byte[] bytes = Files.readAllBytes(outputFile);
        String content = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(content.contains("\u00e9\u00e8\u00ea\u00eb"),
                "UTF-8 characters should be preserved");
    }

    @Test
    void writeOmitsNullValues(@TempDir Path tempDir) throws IOException {
        ObjectNode resource = writer.createObjectNode();
        resource.put("type", "Manifest");
        resource.putNull("summary");

        Path outputFile = tempDir.resolve("manifest.json");
        writer.write(resource, outputFile);

        String content = Files.readString(outputFile, StandardCharsets.UTF_8);
        assertFalse(content.contains("summary"), "Null values should be omitted");
        assertTrue(content.contains("type"), "Non-null values should be included");
    }

    @Test
    void writeOmitsEmptyStrings(@TempDir Path tempDir) throws IOException {
        ObjectNode resource = writer.createObjectNode();
        resource.put("type", "Manifest");
        resource.put("label", "");

        Path outputFile = tempDir.resolve("manifest.json");
        writer.write(resource, outputFile);

        String content = Files.readString(outputFile, StandardCharsets.UTF_8);
        assertFalse(content.contains("label"), "Empty strings should be omitted");
    }

    @Test
    void writeOmitsEmptyArrays(@TempDir Path tempDir) throws IOException {
        ObjectNode resource = writer.createObjectNode();
        resource.put("type", "Manifest");
        resource.putArray("items");

        Path outputFile = tempDir.resolve("manifest.json");
        writer.write(resource, outputFile);

        String content = Files.readString(outputFile, StandardCharsets.UTF_8);
        assertFalse(content.contains("items"), "Empty arrays should be omitted");
    }

    @Test
    void writeOmitsEmptyObjects(@TempDir Path tempDir) throws IOException {
        ObjectNode resource = writer.createObjectNode();
        resource.put("type", "Manifest");
        resource.putObject("metadata");

        Path outputFile = tempDir.resolve("manifest.json");
        writer.write(resource, outputFile);

        String content = Files.readString(outputFile, StandardCharsets.UTF_8);
        assertFalse(content.contains("metadata"), "Empty objects should be omitted");
    }

    @Test
    void writeProducesDeterministicPropertyOrdering(@TempDir Path tempDir) throws IOException {
        ObjectNode resource = writer.createObjectNode();
        resource.put("zebra", "last");
        resource.put("apple", "first");
        resource.put("mango", "middle");

        Path outputFile = tempDir.resolve("manifest.json");
        writer.write(resource, outputFile);

        String content = Files.readString(outputFile, StandardCharsets.UTF_8);
        int appleIdx = content.indexOf("apple");
        int mangoIdx = content.indexOf("mango");
        int zebraIdx = content.indexOf("zebra");
        assertTrue(appleIdx < mangoIdx && mangoIdx < zebraIdx,
                "Properties should be in alphabetical order");
    }

    @Test
    void writeCreatesParentDirectories(@TempDir Path tempDir) throws IOException {
        ObjectNode resource = writer.createObjectNode();
        resource.put("type", "Collection");

        Path outputFile = tempDir.resolve("deep/nested/dir/collection.json");
        writer.write(resource, outputFile);

        assertTrue(Files.exists(outputFile), "Output file should exist");
        String content = Files.readString(outputFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("Collection"));
    }

    @Test
    void writeToStringProducesCompactJson() throws IOException {
        ObjectNode resource = writer.createObjectNode();
        resource.put("type", "Manifest");
        resource.put("id", "http://example.org/manifest/1");

        String json = writer.writeToString(resource);
        assertFalse(json.contains("\n"), "writeToString should produce single-line JSON");
        assertTrue(json.contains("\"type\":\"Manifest\"") || json.contains("\"type\" : \"Manifest\""));
    }

    @Test
    void writeToStringOmitsEmptyValues() throws IOException {
        ObjectNode resource = writer.createObjectNode();
        resource.put("type", "Canvas");
        resource.put("label", "");
        resource.putNull("summary");
        resource.putArray("items");
        resource.putObject("metadata");

        String json = writer.writeToString(resource);
        assertTrue(json.contains("type"), "Non-empty values should be present");
        assertFalse(json.contains("label"), "Empty strings should be omitted");
        assertFalse(json.contains("summary"), "Null values should be omitted");
        assertFalse(json.contains("items"), "Empty arrays should be omitted");
        assertFalse(json.contains("metadata"), "Empty objects should be omitted");
    }

    @Test
    void writeToStringDeterministicOrder() throws IOException {
        ObjectNode resource = writer.createObjectNode();
        resource.put("zoo", "z");
        resource.put("bar", "b");
        resource.put("alpha", "a");

        String json = writer.writeToString(resource);
        int alphaIdx = json.indexOf("alpha");
        int barIdx = json.indexOf("bar");
        int zooIdx = json.indexOf("zoo");
        assertTrue(alphaIdx < barIdx && barIdx < zooIdx,
                "Keys should be in sorted order");
    }

    @Test
    void createObjectNodeReturnsEmptyNode() {
        ObjectNode node = writer.createObjectNode();
        assertNotNull(node);
        assertTrue(node.isEmpty());
    }

    @Test
    void writeHandlesNestedObjects(@TempDir Path tempDir) throws IOException {
        ObjectNode resource = writer.createObjectNode();
        resource.put("type", "Manifest");
        ObjectNode label = resource.putObject("label");
        ArrayNode enLabels = label.putArray("en");
        enLabels.add("Test Label");

        Path outputFile = tempDir.resolve("manifest.json");
        writer.write(resource, outputFile);

        String content = Files.readString(outputFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("\"en\":[\"Test Label\"]") || content.contains("\"en\" : [\"Test Label\"]"));
    }

    @Test
    void writeHandlesNestedObjectsWithEmpties(@TempDir Path tempDir) throws IOException {
        ObjectNode resource = writer.createObjectNode();
        resource.put("type", "Manifest");
        ObjectNode nested = resource.putObject("service");
        nested.put("type", "ImageService2");
        nested.put("profile", "");  // empty - should be omitted in nested

        Path outputFile = tempDir.resolve("manifest.json");
        writer.write(resource, outputFile);

        String content = Files.readString(outputFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("service"), "Non-empty nested object should be kept");
        assertTrue(content.contains("ImageService2"), "Non-empty nested field should be kept");
        assertFalse(content.contains("profile"), "Empty nested field should be omitted");
    }

    @Test
    void writeRemovesNestedObjectThatBecomesEmptyAfterCleaning(@TempDir Path tempDir) throws IOException {
        ObjectNode resource = writer.createObjectNode();
        resource.put("type", "Manifest");
        ObjectNode nested = resource.putObject("metadata");
        nested.put("label", "");  // empty - removed, making nested empty

        Path outputFile = tempDir.resolve("manifest.json");
        writer.write(resource, outputFile);

        String content = Files.readString(outputFile, StandardCharsets.UTF_8);
        assertFalse(content.contains("metadata"),
                "Nested object that becomes empty after cleaning should be omitted");
    }

    @Test
    void roundTripSerializationPreservesValues() throws IOException {
        ObjectNode resource = writer.createObjectNode();
        resource.put("@context", "http://iiif.io/api/presentation/3/context.json");
        resource.put("id", "http://example.org/manifest/1");
        resource.put("type", "Manifest");
        ObjectNode label = resource.putObject("label");
        ArrayNode enLabels = label.putArray("en");
        enLabels.add("Test Manifest");

        String json = writer.writeToString(resource);
        JsonNode parsed = writer.getObjectMapper().readTree(json);

        assertEquals("http://iiif.io/api/presentation/3/context.json",
                parsed.get("@context").asText());
        assertEquals("http://example.org/manifest/1",
                parsed.get("id").asText());
        assertEquals("Manifest", parsed.get("type").asText());
        assertEquals("Test Manifest",
                parsed.get("label").get("en").get(0).asText());
    }

    @Test
    void writeNonEmptyArraysArePreserved(@TempDir Path tempDir) throws IOException {
        ObjectNode resource = writer.createObjectNode();
        resource.put("type", "Collection");
        ArrayNode items = resource.putArray("items");
        ObjectNode item = items.addObject();
        item.put("id", "http://example.org/manifest/1");
        item.put("type", "Manifest");

        Path outputFile = tempDir.resolve("collection.json");
        writer.write(resource, outputFile);

        String content = Files.readString(outputFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("items"), "Non-empty arrays should be preserved");
        assertTrue(content.contains("Manifest"));
    }
}
