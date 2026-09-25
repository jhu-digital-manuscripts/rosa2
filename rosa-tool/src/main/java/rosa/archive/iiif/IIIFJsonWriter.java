package rosa.archive.iiif;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes IIIF Presentation API 3.0 resources as compact single-line JSON with UTF-8 encoding.
 *
 * <p>The writer is configured to produce deterministic output by ordering map entries by keys,
 * and to omit null values, empty strings, empty arrays, and empty objects from the serialized JSON.
 *
 * <p>This class uses Jackson's {@link ObjectNode}/{@link ObjectMapper} approach since the IIIF
 * generation builds JSON trees programmatically rather than from Java model objects.
 */
public final class IIIFJsonWriter {

    private final ObjectMapper mapper;

    /**
     * Creates a new writer with Jackson configured for IIIF 3.0 JSON serialization constraints.
     */
    public IIIFJsonWriter() {
        this.mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configOverride(String.class)
                .setInclude(JsonInclude.Value.construct(JsonInclude.Include.NON_EMPTY, JsonInclude.Include.NON_EMPTY));
    }

    /**
     * Writes a IIIF resource as compact single-line JSON to the specified file.
     *
     * <p>Parent directories are created if they do not exist. The output uses UTF-8 encoding
     * and contains no null values, empty strings, empty arrays, or empty objects.
     * Properties are written in a deterministic order (sorted by key).
     *
     * @param resource   the IIIF resource as a Jackson {@link ObjectNode}
     * @param outputFile the path to write the JSON file to
     * @throws IOException if an I/O error occurs during writing or directory creation
     */
    public void write(ObjectNode resource, Path outputFile) throws IOException {
        Path parent = outputFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        ObjectNode cleaned = removeEmpties(resource);
        try (OutputStream out = Files.newOutputStream(outputFile)) {
            mapper.writeValue(out, sorted(cleaned));
        }
    }

    /**
     * Serializes a IIIF resource to a compact single-line JSON string.
     *
     * <p>The output uses the same configuration as {@link #write(ObjectNode, Path)}:
     * no null values, empty strings, empty arrays, or empty objects, with deterministic
     * property ordering.
     *
     * @param resource the IIIF resource as a Jackson {@link ObjectNode}
     * @return the compact JSON string representation
     * @throws IOException if a serialization error occurs
     */
    public String writeToString(ObjectNode resource) throws IOException {
        ObjectNode cleaned = removeEmpties(resource);
        return mapper.writeValueAsString(sorted(cleaned));
    }

    /**
     * Creates a new empty {@link ObjectNode} using this writer's internal ObjectMapper.
     *
     * <p>Use this method to build IIIF resource JSON trees programmatically.
     *
     * @return a new empty ObjectNode
     */
    public ObjectNode createObjectNode() {
        return mapper.createObjectNode();
    }

    /**
     * Returns the internal {@link ObjectMapper} for advanced use cases such as
     * reading JSON back for round-trip verification.
     *
     * @return the configured ObjectMapper instance
     */
    public ObjectMapper getObjectMapper() {
        return mapper;
    }

    /**
     * Recursively removes empty strings, empty arrays, and empty objects from an ObjectNode.
     */
    private ObjectNode removeEmpties(ObjectNode node) {
        ObjectNode result = mapper.createObjectNode();
        var fields = node.fields();
        while (fields.hasNext()) {
            var entry = fields.next();
            var value = entry.getValue();
            if (value == null || value.isNull()) {
                continue;
            }
            if (value.isTextual() && value.textValue().isEmpty()) {
                continue;
            }
            if (value.isArray() && value.isEmpty()) {
                continue;
            }
            if (value.isObject() && value.isEmpty()) {
                continue;
            }
            if (value.isObject()) {
                ObjectNode cleaned = removeEmpties((ObjectNode) value);
                if (!cleaned.isEmpty()) {
                    result.set(entry.getKey(), cleaned);
                }
            } else {
                result.set(entry.getKey(), value);
            }
        }
        return result;
    }

    /**
     * Returns a sorted copy of the ObjectNode with keys in natural order.
     * Jackson's ORDER_MAP_ENTRIES_BY_KEYS handles Maps but ObjectNode fields
     * are insertion-ordered, so we rebuild with sorted keys.
     * Also recurses into arrays to sort any ObjectNode elements.
     */
    private ObjectNode sorted(ObjectNode node) {
        ObjectNode result = mapper.createObjectNode();
        var fieldNames = new java.util.ArrayList<String>();
        node.fieldNames().forEachRemaining(fieldNames::add);
        java.util.Collections.sort(fieldNames);
        for (String name : fieldNames) {
            var value = node.get(name);
            if (value.isObject()) {
                result.set(name, sorted((ObjectNode) value));
            } else if (value.isArray()) {
                result.set(name, sortedArray((com.fasterxml.jackson.databind.node.ArrayNode) value));
            } else {
                result.set(name, value);
            }
        }
        return result;
    }

    /**
     * Returns a copy of the ArrayNode with any ObjectNode elements recursively sorted.
     */
    private com.fasterxml.jackson.databind.node.ArrayNode sortedArray(com.fasterxml.jackson.databind.node.ArrayNode array) {
        com.fasterxml.jackson.databind.node.ArrayNode result = mapper.createArrayNode();
        for (var element : array) {
            if (element.isObject()) {
                result.add(sorted((ObjectNode) element));
            } else if (element.isArray()) {
                result.add(sortedArray((com.fasterxml.jackson.databind.node.ArrayNode) element));
            } else {
                result.add(element);
            }
        }
        return result;
    }
}
