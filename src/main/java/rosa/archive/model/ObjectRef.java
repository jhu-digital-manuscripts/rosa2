package rosa.archive.model;

import java.util.Objects;

/**
 * A reference to a named entity with an optional URI link.
 * Used for authors, readers, and other referenced objects in bibliographic data.
 */
public class ObjectRef {

    private String name;
    private String uri;

    /**
     * Creates an empty ObjectRef.
     */
    public ObjectRef() {}

    /**
     * Creates an ObjectRef with the given name and URI.
     *
     * @param name the display name
     * @param uri  the URI, or {@code null} if not available
     */
    public ObjectRef(String name, String uri) {
        this.name = name;
        this.uri = uri;
    }

    /**
     * Returns the display name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the display name.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the URI for this reference.
     *
     * @return the URI, or {@code null} if not available
     */
    public String getUri() {
        return uri;
    }

    /**
     * Sets the URI for this reference.
     *
     * @param uri the URI
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ObjectRef that)) return false;
        return Objects.equals(name, that.name) &&
                Objects.equals(uri, that.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, uri);
    }

    @Override
    public String toString() {
        return "ObjectRef{" +
                "name='" + name + '\'' +
                ", uri='" + uri + '\'' +
                '}';
    }
}
