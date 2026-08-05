package rosa.archive.iiif;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates service/jhsearch.json files per collection for the JHSearch service.
 *
 * <p>The JSON structure contains fields (with optional subfields and enumerated values),
 * categories, default-fields, and the Opensearch endpoint URL that clients can use
 * for search queries.
 *
 * <p>Field names and category names correspond to actual Opensearch index field names
 * defined in the {@code opensearch/} directory so clients can construct valid queries.
 */
public final class JHSearchInfoGenerator {

    private final ObjectMapper mapper;

    /**
     * Language sub-fields available in the annotations index for multi-language text fields.
     * These are the sub-field names under text, translation, emphasis, cross_reference, anchor_text.
     */
    private static final List<String> ANNOTATION_LANG_SUBFIELDS = List.of("en", "fr", "la", "it", "el", "es", "de", "ofr");

    /**
     * Language sub-fields available in the manifests index for the title field.
     */
    private static final List<String> MANIFEST_TITLE_SUBFIELDS = List.of("en", "fr", "ofr", "la", "el", "it", "es");

    // Per-collection search fields (using opensearch field names)
    private static final Map<String, List<String>> SEARCH_FIELDS = Map.of(
            "rose", List.of("description", "repository", "locations", "text", "title", "translation"),
            "pizan", List.of("description", "repository", "locations", "title", "text", "translation"),
            "aor", List.of("text", "symbols", "people", "locations", "language",
                    "books", "method", "emphasis", "cross_reference", "anchor_text",
                    "translation", "hand", "annotator"),
            "top", List.of("description", "title", "people", "locations", "repository", "text"),
            "dlmm", List.of("description", "title", "people", "locations", "repository", "text", "translation")
    );

    // Per-collection search categories (using actual opensearch field names for faceting)
    private static final Map<String, List<String>> SEARCH_CATEGORIES = Map.of(
            "rose", List.of("current_location", "date", "number_of_illustrations", "num_pages", "origin", "type", "has_transcription"),
            "pizan", List.of("current_location", "date", "number_of_illustrations", "num_pages", "origin", "type", "has_transcription"),
            "aor", List.of("authors", "current_location", "date", "num_pages", "origin"),
            "top", List.of("authors", "current_location", "date", "repository"),
            "dlmm", List.of("authors", "current_location", "number_of_illustrations", "num_pages", "origin", "type", "has_transcription")
    );

    /**
     * Creates a new generator with the given ObjectMapper.
     *
     * @param mapper the Jackson ObjectMapper for building JSON structures
     */
    public JHSearchInfoGenerator(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Generates the service/jhsearch.json structure for a given collection.
     *
     * @param collectionId  the collection identifier (e.g., "rose", "aor", "pizan")
     * @param opensearchUrl the Opensearch _search endpoint URL
     * @return the JSON structure as an ObjectNode
     */
    public ObjectNode generate(String collectionId, String opensearchUrl) {
        ObjectNode root = mapper.createObjectNode();

        List<String> fieldNames = SEARCH_FIELDS.getOrDefault(collectionId, List.of());
        List<String> categoryNames = SEARCH_CATEGORIES.getOrDefault(collectionId, List.of());

        // fields array
        ArrayNode fieldsArray = mapper.createArrayNode();
        for (String fieldName : fieldNames) {
            FieldMetadata meta = getFieldMetadata(fieldName);
            if (meta != null) {
                ObjectNode fieldNode = mapper.createObjectNode();
                fieldNode.put("name", fieldName);
                fieldNode.put("label", meta.label);
                fieldNode.put("description", meta.description);

                // Add subfields if this field has language sub-fields
                if (meta.subfields != null && !meta.subfields.isEmpty()) {
                    ArrayNode subfieldsArray = mapper.createArrayNode();
                    for (String subfield : meta.subfields) {
                        subfieldsArray.add(subfield);
                    }
                    fieldNode.set("subfields", subfieldsArray);
                }

                if (meta.values != null && !meta.values.isEmpty()) {
                    ArrayNode valuesArray = mapper.createArrayNode();
                    for (Map.Entry<String, String> entry : meta.values.entrySet()) {
                        ObjectNode valueNode = mapper.createObjectNode();
                        valueNode.put("value", entry.getKey());
                        valueNode.put("label", entry.getValue());
                        valuesArray.add(valueNode);
                    }
                    fieldNode.set("values", valuesArray);
                }
                fieldsArray.add(fieldNode);
            }
        }
        root.set("fields", fieldsArray);

        // categories array
        ArrayNode categoriesArray = mapper.createArrayNode();
        for (String categoryName : categoryNames) {
            CategoryMetadata meta = getCategoryMetadata(categoryName);
            if (meta != null) {
                ObjectNode catNode = mapper.createObjectNode();
                catNode.put("name", meta.fieldName);
                catNode.put("label", meta.label);
                categoriesArray.add(catNode);
            }
        }
        root.set("categories", categoriesArray);

        // default-fields array
        ArrayNode defaultFields = mapper.createArrayNode();
        for (String fieldName : fieldNames) {
            defaultFields.add(fieldName);
        }
        root.set("default-fields", defaultFields);

        // opensearch endpoint
        root.put("opensearch", opensearchUrl);

        return root;
    }

    private FieldMetadata getFieldMetadata(String fieldName) {
        return switch (fieldName) {
            // Annotations index: multi-language text fields
            case "text" -> new FieldMetadata("Text",
                    "Search the text of an item. This can include transcriptions, marginalia, and translations.",
                    ANNOTATION_LANG_SUBFIELDS, null);
            case "translation" -> new FieldMetadata("Translation",
                    "Search within translated text of annotations.",
                    ANNOTATION_LANG_SUBFIELDS, null);
            case "emphasis" -> new FieldMetadata("Emphasis",
                    "Words or phrases within the readers marginal notes that have been underlined or otherwise emphasized.",
                    ANNOTATION_LANG_SUBFIELDS, null);
            case "cross_reference" -> new FieldMetadata("Cross Reference",
                    "Quotes from sources not explicitly identified by the reader.",
                    ANNOTATION_LANG_SUBFIELDS, null);
            case "anchor_text" -> new FieldMetadata("Anchor Text",
                    "Text from drawings and tables.",
                    ANNOTATION_LANG_SUBFIELDS, null);

            // Manifests index: multi-language title field
            case "title" -> new FieldMetadata("Title", "Search titles of items.",
                    MANIFEST_TITLE_SUBFIELDS, null);

            // Annotations index: keyword fields
            case "symbols" -> new FieldMetadata("Symbol",
                    "Simple drawings that carry some abstract and consistent meaning.",
                    null, symbolValues());
            case "people" -> new FieldMetadata("People",
                    "Search for names of people within metadata and transcriptions.",
                    null, null);
            case "locations" -> new FieldMetadata("Place",
                    "Search for places and locations within metadata and transcriptions.",
                    null, null);
            case "books" -> new FieldMetadata("Book",
                    "Titles of books that are referenced in text or annotations.",
                    null, null);
            case "language" -> new FieldMetadata("Language",
                    "Language of the annotation.",
                    null, languageValues());
            case "method" -> new FieldMetadata("Method",
                    "Implement used to create mark or underline.",
                    null, methodValues());
            case "hand" -> new FieldMetadata("Hand",
                    "The hand in which an annotation was written.",
                    null, null);
            case "annotator" -> new FieldMetadata("Annotator",
                    "Author of annotations.",
                    null, null);

            // Manifests index: text fields
            case "description" -> new FieldMetadata("Description",
                    "Search within the metadata for the collection.",
                    null, null);
            case "repository" -> new FieldMetadata("Repository",
                    "Search for names of repositories in which manuscripts are currently held.",
                    null, null);

            default -> null;
        };
    }

    private CategoryMetadata getCategoryMetadata(String categoryName) {
        return switch (categoryName) {
            case "authors" -> new CategoryMetadata("authors", "Author");
            case "current_location" -> new CategoryMetadata("current_location", "Current Location");
            case "date" -> new CategoryMetadata("date", "Date");
            case "origin" -> new CategoryMetadata("origin", "Origin");
            case "type" -> new CategoryMetadata("type", "Type");
            case "num_pages" -> new CategoryMetadata("num_pages", "Number of Pages");
            case "number_of_illustrations" -> new CategoryMetadata("number_of_illustrations", "Number of Illustrations");
            case "has_transcription" -> new CategoryMetadata("has_transcription", "Transcription");
            case "repository" -> new CategoryMetadata("repository", "Repository");
            default -> null;
        };
    }

    private static Map<String, String> languageValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("en", "English");
        values.put("es", "Spanish");
        values.put("it", "Italian");
        values.put("el", "Greek");
        values.put("la", "Latin");
        values.put("fr", "French");
        values.put("de", "German");
        values.put("ofr", "Old French");
        return values;
    }

    private static Map<String, String> methodValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("pen", "Pen");
        values.put("chalk", "Chalk");
        values.put("pencil", "Pencil");
        values.put("scoring", "Scoring");
        values.put("typewriter", "Typewriter");
        return values;
    }

    private static Map<String, String> symbolValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("Asterisk", "Asterisk");
        values.put("Bisected_circle", "Bisected Circle");
        values.put("Crown", "Crown");
        values.put("JC", "JC");
        values.put("HT", "HT");
        values.put("Hieroglyphic_Monad", "Hieroglyphic Monad");
        values.put("Jupiter", "Jupiter");
        values.put("LL", "LL");
        values.put("Mars", "Mars");
        values.put("Mercury", "Mercury");
        values.put("Moon", "Moon");
        values.put("Opposite_planets", "Opposite Planets");
        values.put("Conjunction", "Conjunction");
        values.put("Salt", "Salt");
        values.put("Saturn", "Saturn");
        values.put("Florilegium", "Florilegium");
        values.put("Square", "Square");
        values.put("Trine", "Trine");
        values.put("SS", "SS");
        values.put("Sulfur", "Sulfur");
        values.put("Sun", "Sun");
        values.put("Venus", "Venus");
        values.put("Aries", "Aries");
        values.put("Cancer", "Cancer");
        values.put("Libra", "Libra");
        values.put("Capricorn", "Capricorn");
        values.put("Taurus", "Taurus");
        values.put("Leo", "Leo");
        values.put("Scorpio", "Scorpio");
        values.put("Aquarius", "Aquarius");
        values.put("Gemini", "Gemini");
        values.put("Virgo", "Virgo");
        values.put("Sagittarius", "Sagittarius");
        values.put("Pices", "Pices");
        values.put("North_Node", "North Node");
        values.put("South_Node", "South Node");
        values.put("Sextile", "Sextile");
        values.put("Phi", "Phi");
        values.put("Simeiosi", "Simeiosi");
        return values;
    }

    private record FieldMetadata(String label, String description, List<String> subfields, Map<String, String> values) {}

    private record CategoryMetadata(String fieldName, String label) {}
}
