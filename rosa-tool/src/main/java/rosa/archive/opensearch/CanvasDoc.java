package rosa.archive.opensearch;

import java.util.List;
import java.util.Map;

/**
 * An Opensearch document representing a single page (canvas) in the {@code canvas} index.
 *
 * <p>Each canvas corresponds to one page image within a book. All annotations targeting
 * this canvas are merged into the document with per-type independently searchable fields.
 *
 * <p>Multi-language text fields are represented as {@code Map<String, String>} where keys
 * are language codes (en, fr, la, it, el, es, de, ofr) and values are the text content.
 * Content from multiple annotations of the same type is concatenated.
 *
 * <p>Fields like {@code mark}, {@code symbol}, {@code drawing}, {@code calculation},
 * {@code graph}, and {@code table} also have a {@code keyword} entry in their map for
 * enumerated type/name values (searched via e.g. {@code mark.keyword}).
 *
 * @param id                  unique canvas ID (collection.book.image)
 * @param manifestId          parent manifest/book ID
 * @param collectionId        all ancestor collection IDs
 * @param label               page label (pagination, signature, or image name)
 * @param imageName           short image identifier
 * @param position            1-based page position in the book
 * @param marginalia          marginalia text routed by language
 * @param underline           underline referenced text routed by language
 * @param mark                mark names (keyword) and referenced text routed by language
 * @param symbol              symbol names (keyword) and referenced text routed by language
 * @param errata              errata text routed by language
 * @param numeral             numeral text routed by language
 * @param drawing             drawing types (keyword) and text routed by language
 * @param emphasis            emphasized text from marginalia routed by language
 * @param crossReference      cross-reference text routed by language
 * @param calculation         calculation types (keyword) and text routed by language
 * @param graph               graph types (keyword) and text routed by language
 * @param table               table types (keyword) and text routed by language
 * @param transcription       transcription text routed by language
 * @param illustration        illustration description text routed by language
 * @param translation         translation text routed by language
 * @param anchorText          anchor text from drawings/tables routed by language
 * @param people              people referenced across all annotations
 * @param books               books referenced across all annotations
 * @param locations           locations referenced across all annotations
 * @param symbols             symbol names from all annotations
 * @param method              methods used (pen, chalk, etc.)
 * @param hand                hands identified
 * @param annotator           annotator(s) for this page
 * @param language            all annotation languages on this canvas
 * @param marginaliaLanguage  languages used within marginalia specifically
 * @param topic               marginalia topics
 * @param charName            character names (from illustrations)
 */
public record CanvasDoc(
        String id,
        String manifestId,
        List<String> collectionId,
        String label,
        String imageName,
        int position,
        Map<String, String> marginalia,
        Map<String, String> underline,
        Map<String, String> mark,
        Map<String, String> symbol,
        Map<String, String> errata,
        Map<String, String> numeral,
        Map<String, String> drawing,
        Map<String, String> emphasis,
        Map<String, String> crossReference,
        Map<String, String> calculation,
        Map<String, String> graph,
        Map<String, String> table,
        Map<String, String> transcription,
        Map<String, String> illustration,
        Map<String, String> translation,
        Map<String, String> anchorText,
        List<String> people,
        List<String> books,
        List<String> locations,
        List<String> symbols,
        List<String> method,
        List<String> hand,
        List<String> annotator,
        List<String> language,
        List<String> marginaliaLanguage,
        List<String> topic,
        List<String> charName) {}
