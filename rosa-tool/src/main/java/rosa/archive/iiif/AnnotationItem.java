package rosa.archive.iiif;

import java.util.List;

/**
 * The content of one commenting annotation before it is serialized.
 *
 * @param styleClasses the CSS classes for the body {@code styleClass}, in order
 * @param html         the body HTML, following {@link AnnotationHtml} rules
 * @param language     the body language code, or null if unknown or mixed
 */
public record AnnotationItem(List<String> styleClasses, String html, String language) {

    /**
     * Creates an item, copying the class list.
     */
    public AnnotationItem {
        styleClasses = List.copyOf(styleClasses);
    }
}
