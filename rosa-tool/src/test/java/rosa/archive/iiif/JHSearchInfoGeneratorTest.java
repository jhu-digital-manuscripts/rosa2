package rosa.archive.iiif;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JHSearchInfoGeneratorTest {

    private final JHSearchInfoGenerator generator = new JHSearchInfoGenerator(new ObjectMapper());

    @Test
    void enumeratedValuesAreSortedByLabel() {
        ObjectNode root = generator.generate("aor", "https://example.org/_search");

        int fieldsWithValues = 0;
        for (JsonNode field : root.get("fields")) {
            if (!field.has("values")) continue;
            fieldsWithValues++;

            List<String> labels = new ArrayList<>();
            field.get("values").forEach(v -> labels.add(v.get("label").asText()));
            List<String> sorted = new ArrayList<>(labels);
            sorted.sort(String.CASE_INSENSITIVE_ORDER);
            assertEquals(sorted, labels, "values of " + field.get("name").asText() + " should be sorted");
        }
        // mark, symbol, drawing, language, marginalia_language, method
        assertEquals(6, fieldsWithValues);
    }
}
