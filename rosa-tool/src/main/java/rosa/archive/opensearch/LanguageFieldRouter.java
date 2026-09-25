package rosa.archive.opensearch;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;

/**
 * Routes text content to the appropriate language-specific sub-field in an Opensearch document.
 *
 * <p>The Opensearch annotations index uses a multi-field pattern where textual content
 * is stored in language-qualified sub-fields (e.g., {@code text.fr}, {@code text.en})
 * so that language-appropriate analysis is applied at index time.
 *
 * <p>Supported languages: en, fr, la, it, el, es, de, ofr.
 * Unrecognized language codes fall back to the {@code .en} sub-field.
 */
public final class LanguageFieldRouter {

    private static final Map<String, String> LANG_MAP = Map.of(
            "en", "en",
            "fr", "fr",
            "la", "la",
            "it", "it",
            "el", "el",
            "es", "es",
            "de", "de",
            "ofr", "ofr"
    );

    private LanguageFieldRouter() {
        // utility class
    }

    /**
     * Routes text content to the appropriate language sub-field on a document node.
     *
     * <p>Sets {@code doc[fieldName.subField] = content} where the sub-field is determined
     * by the language code. For example, {@code routeField(doc, "text", "fr", "bonjour")}
     * sets {@code doc["text.fr"] = "bonjour"}.
     *
     * <p>If the language code is not recognized, falls back to the {@code .en} sub-field.
     * If content is null or blank, no field is set.
     *
     * @param doc       the Jackson ObjectNode to set the field on
     * @param fieldName the base field name (e.g., "text", "translation")
     * @param langCode  the language code (e.g., "fr", "la")
     * @param content   the text content to store
     */
    public static void routeField(ObjectNode doc, String fieldName, String langCode, String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        String subField = resolveSubField(langCode);
        String key = fieldName + "." + subField;
        if (doc.has(key)) {
            // Append to existing content with a space separator
            String existing = doc.get(key).asText();
            doc.put(key, existing + " " + content);
        } else {
            doc.put(key, content);
        }
    }

    /**
     * Returns the sub-field suffix for a language code.
     *
     * <p>Falls back to {@code "en"} for unrecognized language codes.
     *
     * @param langCode the language code to resolve
     * @return the sub-field suffix (e.g., "fr", "en", "ofr")
     */
    public static String resolveSubField(String langCode) {
        if (langCode == null) {
            return "en";
        }
        return LANG_MAP.getOrDefault(langCode.toLowerCase(), "en");
    }
}
