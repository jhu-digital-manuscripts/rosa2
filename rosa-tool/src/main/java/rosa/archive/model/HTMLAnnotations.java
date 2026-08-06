package rosa.archive.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A set of HTML annotations targeting books, images, or collections.
 * The HTML content is simple and intended to be embedded in an existing HTML document.
 */
public class HTMLAnnotations implements HasId {

    private final Map<String, String> annotations;
    private String id;

    /**
     * Creates an empty HTMLAnnotations collection.
     */
    public HTMLAnnotations() {
        this.annotations = new HashMap<>();
    }

    /**
     * Sets an HTML annotation for the given target.
     *
     * @param targetId the target identifier (book, image, or collection id)
     * @param html     the HTML content
     */
    public void setAnnotation(String targetId, String html) {
        annotations.put(targetId, html);
    }

    /**
     * Returns the HTML annotation for the given target.
     *
     * @param targetId the target identifier
     * @return the HTML content, or {@code null} if no annotation exists for this target
     */
    public String getAnnotation(String targetId) {
        return annotations.get(targetId);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the number of annotations.
     *
     * @return the annotation count
     */
    public int size() {
        return annotations.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HTMLAnnotations that)) return false;
        return Objects.equals(id, that.id) &&
                Objects.equals(annotations, that.annotations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, annotations);
    }

    @Override
    public String toString() {
        return "HTMLAnnotations{" +
                "id='" + id + '\'' +
                ", annotations=" + annotations +
                '}';
    }
}
