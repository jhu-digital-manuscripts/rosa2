package rosa.archive.model;

import org.w3c.dom.Element;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A prose description of a book.
 */
public final class BookDescription implements HasId, Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private Map<String, Element> notes;

    public BookDescription() {
        this.notes = new HashMap<>();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public Map<String, Element> getNotes() {
        return notes;
    }

    public void setNotes(Map<String, Element> notes) {
        this.notes = notes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BookDescription that = (BookDescription) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (notes != null ? !notes.equals(that.notes) : that.notes != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (notes != null ? notes.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "BookDescription{" +
                "id='" + id + '\'' +
                ", notes=" + notes +
                '}';
    }
}
