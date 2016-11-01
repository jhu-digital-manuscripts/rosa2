package rosa.archive.model;

import java.io.Serializable;

/**
 * Prose description about a book.
 */
public final class BookDescription implements HasId, Serializable {
    public enum TOPIC {
        IDENTIFICATION, BASIC_INFORMATION, MATERIAL, QUIRES, LAYOUT, SCRIPT, DECORATION, BINDING,
        HISTORY, TEXT, AUTHOR
    }

    private static final long serialVersionUID = 1L;

    private String description;
    private String id;

    /** Create a new BookDescription */
    public BookDescription() {}

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getXML() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookDescription)) return false;

        BookDescription that = (BookDescription) o;

        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        return !(id != null ? !id.equals(that.id) : that.id != null);

    }

    @Override
    public int hashCode() {
        int result = description != null ? description.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "BookDescription{" +
                "description=" + description +
                ", id='" + id + '\'' +
                '}';
    }
}
