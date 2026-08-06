package rosa.archive.opensearch;

import java.util.List;
import java.util.Map;

/**
 * An Opensearch document representing a single annotation in the {@code annotations} index.
 *
 * <p>Captures all annotation data including type-specific fields, multi-language text content
 * routed into language sub-fields, and reference arrays for people, books, and locations.
 *
 * @param id             the unique document identifier
 * @param canvasId       the identifier of the canvas (page) this annotation targets
 * @param manifestId     the identifier of the manifest (book) containing this annotation
 * @param collectionId   the identifier of the collection containing this annotation
 * @param type           the annotation type (e.g. marginalia, underline, mark, symbol, drawing,
 *                       errata, numeral, transcription, illustration, calculation, graph, table)
 * @param language       the primary language of this annotation
 * @param imageName      the short image identifier for page-based lookups
 * @param annotator      the reader who made this annotation
 * @param topic          the topic associated with marginalia annotations
 * @param color          the color attribute of the annotation
 * @param orientation    the orientation of drawings, graphs, or calculations
 * @param method         the method used for the annotation
 * @param hand           the hand (principal or other) that wrote the annotation
 * @param markName       the mark type name (e.g. plus_sign, dash) for mark annotations
 * @param text           the text content routed by language (e.g. {@code {"en": "content"}})
 * @param translation    the translation content routed by language
 * @param emphasis       emphasized or underlined text within marginalia, routed by language
 * @param crossReference people and titles from XRef elements, routed by language
 * @param anchorText     anchor text from drawings and tables, routed by language
 * @param people         people referenced in the annotation
 * @param books          books referenced in the annotation
 * @param locations      locations referenced in the annotation
 * @param symbols        symbol names referenced in the annotation
 */
public record AnnotationDoc(
        String id,
        String canvasId,
        String manifestId,
        String collectionId,
        String type,
        String language,
        String imageName,
        String annotator,
        String topic,
        String color,
        String orientation,
        String method,
        String hand,
        String markName,
        Map<String, String> text,
        Map<String, String> translation,
        Map<String, String> emphasis,
        Map<String, String> crossReference,
        Map<String, String> anchorText,
        List<String> people,
        List<String> books,
        List<String> locations,
        List<String> symbols) {}
