package rosa.archive.iiif;

/**
 * The body of a IIIF 3.0 annotation, carrying the content to be associated with a target.
 *
 * <p>Used within Annotation Page items to describe textual or other content
 * painted onto or associated with a Canvas.
 *
 * @param type     the body type (e.g. "TextualBody", "Image")
 * @param value    the content value (text content or image URI)
 * @param format   the media type of the body (e.g. "text/html", "image/jpeg")
 * @param language the BCP 47 language tag of the textual content, or {@code null} if not applicable
 */
public record AnnotationBody(String type, String value, String format, String language) {}
