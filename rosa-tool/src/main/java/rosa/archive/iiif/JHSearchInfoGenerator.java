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
 * <p>The JSON structure contains fields (with optional has_subfields flag and enumerated values),
 * categories, default-fields, and the Opensearch endpoint URL that clients can use
 * for search queries.
 *
 * <p>Field names and category names correspond to actual Opensearch index field names
 * defined in the {@code opensearch/} directory so clients can construct valid queries.
 *
 * <p>The system uses two indexes: {@code manifest} (one doc per book) and {@code canvas}
 * (one doc per page with all annotations merged in). Fields that have subfields (language
 * variants or keyword) are marked with {@code has_subfields: true}. Clients should use
 * {@code field.*} wildcard syntax in multi_match queries to search all subfields.
 */
public final class JHSearchInfoGenerator {

    private final ObjectMapper mapper;

    // Per-collection search fields (using opensearch field names from the canvas index)
    private static final Map<String, List<String>> SEARCH_FIELDS = Map.of(
            "rose", List.of("description", "repository", "locations", "illustration",
                    "char_name", "transcription"),
            "pizan", List.of("description", "repository", "locations", "title",
                    "transcription"),
            "aor", List.of("marginalia", "symbol", "underline", "mark",
                    "books", "people", "locations", "language", "marginalia_language",
                    "numeral", "drawing", "errata", "emphasis", "cross_reference",
                    "method", "calculation", "graph", "table", "hand", "annotator"),
            "dlmm", List.of("description", "title", "people", "locations", "repository",
                    "transcription")
    );

    // Per-collection search categories (using actual opensearch field names for faceting)
    private static final Map<String, List<String>> SEARCH_CATEGORIES = Map.of(
            "rose", List.of("current_location", "date", "num_illustrations", "num_pages", "origin", "type", "has_transcription"),
            "pizan", List.of("current_location", "date", "num_illustrations", "num_pages", "origin", "type", "has_transcription"),
            "aor", List.of("authors", "current_location", "date", "num_pages", "origin"),
            "dlmm", List.of("current_location", "date", "num_illustrations", "num_pages", "origin", "has_transcription", "type")
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

                // Add has_subfields boolean if this field has language sub-fields or keyword sub-field
                if (meta.hasSubfields) {
                    fieldNode.put("has_subfields", true);
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
                if (meta.quantizeInterval != null) {
                    catNode.put("quantize-interval", meta.quantizeInterval);
                }
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
            // Canvas index: per-type multi-language text fields (has_subfields = true)
            case "marginalia" -> new FieldMetadata("Marginalia",
                    "Notes written by a reader.",
                    true, null);
            case "underline" -> new FieldMetadata("Underline",
                    "Words or phrases in the printed text that have been underlined.",
                    true, null);
            case "mark" -> new FieldMetadata("Mark",
                    "Pen marks made on a page that may not have consistent abstract meaning. Search mark.keyword for specific mark types, or mark.* for referenced text.",
                    true, markValues());
            case "symbol" -> new FieldMetadata("Symbol",
                    "Simple drawings that carry some abstract and consistent meaning. Search symbol.keyword for specific symbols, or symbol.* for referenced text.",
                    true, symbolValues());
            case "errata" -> new FieldMetadata("Errata",
                    "Corrections made by a reader to the printed text.",
                    true, null);
            case "numeral" -> new FieldMetadata("Numeral",
                    "Numbers written in the book.",
                    true, null);
            case "drawing" -> new FieldMetadata("Drawing",
                    "Drawings or diagrams. Search drawing.keyword for specific drawing types, or drawing.* for text.",
                    true, drawingValues());
            case "emphasis" -> new FieldMetadata("Emphasis",
                    "Words or phrases within the readers marginal notes that have been underlined or otherwise emphasized.",
                    true, null);
            case "cross_reference" -> new FieldMetadata("Cross Reference",
                    "Quotes from sources not explicitly identified by the reader.",
                    true, null);
            case "calculation" -> new FieldMetadata("Calculation",
                    "Search through annotations used as calculations.",
                    true, null);
            case "graph" -> new FieldMetadata("Graph",
                    "Search through graphs.",
                    true, null);
            case "table" -> new FieldMetadata("Table",
                    "Search through table annotations.",
                    true, null);
            case "transcription" -> new FieldMetadata("Transcription",
                    "Search within transcriptions of manuscript texts.",
                    true, null);
            case "illustration" -> new FieldMetadata("Illustrations",
                    "Search within the descriptions of illustrations.",
                    true, null);
            case "translation" -> new FieldMetadata("Translation",
                    "Search within translated text.",
                    true, null);
            case "anchor_text" -> new FieldMetadata("Anchor Text",
                    "Text from drawings and tables.",
                    true, null);

            // Manifest index: multi-language title field (has_subfields = true)
            case "title" -> new FieldMetadata("Title", "Search titles of items.",
                    true, null);

            // Canvas index: keyword fields (no subfields)
            case "people" -> new FieldMetadata("People",
                    "Search for names of people within metadata and transcriptions.",
                    false, null);
            case "locations" -> new FieldMetadata("Place",
                    "Search for places and locations within metadata and transcriptions.",
                    false, null);
            case "books" -> new FieldMetadata("Book",
                    "Titles of books that are referenced in text or annotations.",
                    false, null);
            case "language" -> new FieldMetadata("Language",
                    "Language of the annotations.",
                    false, languageValues());
            case "marginalia_language" -> new FieldMetadata("Marginalia Language",
                    "Language used specifically within marginalia text.",
                    false, languageValues());
            case "method" -> new FieldMetadata("Method",
                    "Implement used to create mark or underline.",
                    false, methodValues());
            case "hand" -> new FieldMetadata("Hand",
                    "The hand in which an annotation was written.",
                    false, null);
            case "annotator" -> new FieldMetadata("Annotator",
                    "Author of annotations.",
                    false, null);
            case "char_name" -> new FieldMetadata("Character Names",
                    "Search names of characters in the Roman de la Rose.",
                    false, null);

            // Manifest index: text fields (no subfields)
            case "description" -> new FieldMetadata("Description",
                    "Search within the metadata for the collection.",
                    false, null);
            case "repository" -> new FieldMetadata("Repository",
                    "Search for names of repositories in which manuscripts are currently held.",
                    false, null);

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
            case "num_pages" -> new CategoryMetadata("num_pages", "Number of Pages", 100);
            case "num_illustrations" -> new CategoryMetadata("num_illustrations", "Number of Illustrations", 10);
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

    private static Map<String, String> markValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("ampersand", "Ampersand");
        values.put("apostrophe", "Apostrophe");
        values.put("arrow", "Arrow");
        values.put("box", "Box");
        values.put("bracket", "Bracket");
        values.put("circumflex", "Circumflex");
        values.put("colon", "Colon");
        values.put("comma", "Comma");
        values.put("dash", "Dash");
        values.put("diacritic", "Diacritic");
        values.put("dot", "Dot");
        values.put("double_vertical_bar", "Double Vertical Bar");
        values.put("equal_sign", "Equal Sign");
        values.put("est_mark", "Est Mark");
        values.put("hash", "Hash");
        values.put("horizontal_bar", "Horizontal Bar");
        values.put("page_break", "Page Break");
        values.put("pen_trial", "Pen Trial");
        values.put("pin", "Pin");
        values.put("plus_sign", "Plus Sign");
        values.put("quotation_mark", "Quotation Mark");
        values.put("quattuorpunctus", "Quattuorpunctus");
        values.put("quattuorpunctus_with_tail", "Quattuorpunctus with Tail");
        values.put("scribble", "Scribble");
        values.put("section_sign", "Section Sign");
        values.put("semicolon", "Semicolon");
        values.put("slash", "Slash");
        values.put("straight_quotation_mark", "Straight Quotation Mark");
        values.put("small_circle", "Small Circle");
        values.put("tick", "Tick");
        values.put("tilde", "Tilde");
        values.put("triple_dash", "Triple Dash");
        values.put("tripunctus", "Tripunctus");
        values.put("tripunctus_with_tail", "Tripunctus with Tail");
        values.put("duopunctus_with_antenna", "Duopunctus with Antenna");
        values.put("vertical_bar", "Vertical Bar");
        values.put("X_sign", "X Sign");
        values.put("dagger", "Dagger");
        values.put("quinquepunctus", "Quinquepunctus");
        values.put("arrowhead", "Arrowhead");
        values.put("Ichthys", "Ichthys");
        values.put("w_mark", "W Mark");
        values.put("guillemet", "Guillemet");
        values.put("lightening_bolt", "Lightening Bolt");
        values.put("hook", "Hook");
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
        values.put("Unidentified", "Unidentified");
        return values;
    }

    private static Map<String, String> drawingValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("arrow", "Arrow");
        values.put("atoms", "Atoms");
        values.put("cone", "Cone");
        values.put("pyramid", "Pyramid");
        values.put("egg", "Egg");
        values.put("grave", "Grave");
        values.put("axe", "Axe");
        values.put("face", "Face");
        values.put("heart", "Heart");
        values.put("manicule", "Manicule");
        values.put("mountain", "Mountain");
        values.put("florilegium", "Florilegium");
        values.put("crown", "Crown");
        values.put("coat_of_arms", "Coat of Arms");
        values.put("scientific_instrument", "Scientific Instrument");
        values.put("animal", "Animal");
        values.put("chain", "Chain");
        values.put("canon", "Canon");
        values.put("divining rod", "Divining Rod");
        values.put("shield", "Shield");
        values.put("map", "Map");
        values.put("saddle", "Saddle");
        values.put("church", "Church");
        values.put("star", "Star");
        values.put("sword", "Sword");
        values.put("house", "House");
        values.put("ship", "Ship");
        values.put("dragon", "Dragon");
        values.put("person", "Person");
        values.put("scroll", "Scroll");
        values.put("triangle", "Triangle");
        values.put("one_point_perspective_drawing", "One Point Perspective Drawing");
        values.put("geometric_diagram", "Geometric Diagram");
        values.put("sceptre", "Sceptre");
        return values;
    }

    private record FieldMetadata(String label, String description, boolean hasSubfields,
                                  Map<String, String> values) {}

    private record CategoryMetadata(String fieldName, String label, Integer quantizeInterval) {
        CategoryMetadata(String fieldName, String label) {
            this(fieldName, label, null);
        }
    }
}
