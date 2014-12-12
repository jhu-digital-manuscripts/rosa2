package rosa.archive.model;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
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

    public String getNote(String id) {
        return asString(notes.get(id));
    }

    public Map<String, String> getNoteStrings() {
        Map<String, String> strs = new HashMap<>();
        for (String key : notes.keySet()) {
            strs.put(key, getNote(key));
        }
        return strs;
    }

    public void setNotes(Map<String, Element> notes) {
        this.notes = notes;
    }

    private String asString(Element el) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            write(el, baos);
            return baos.toString();
        } catch (TransformerException e) {
            return "";
        }
    }

    /**
     * @param doc document
     * @param out output stream
     */
    private void write(Node doc, OutputStream out) throws TransformerException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

        transformer.transform(
                new DOMSource(doc),
                new StreamResult(out)
        );
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
