package rosa.archive.model.aor;

import java.io.Serializable;

/**
 *
 */
public class XRef implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String person;
    private String title;
    private String text;

    public XRef() {

    }

    public XRef(String person, String title, String text) {
        this.person = person;
        this.title = title;
        this.text = text;
    }

    public String getPerson() {
        return person;
    }

    public void setPerson(String person) {
        this.person = person;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        XRef xRef = (XRef) o;

        if (person != null ? !person.equals(xRef.person) : xRef.person != null) return false;
        if (title != null ? !title.equals(xRef.title) : xRef.title != null) return false;
        if (text != null ? !text.equals(xRef.text) : xRef.text != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = person != null ? person.hashCode() : 0;
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (text != null ? text.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "XRef{" +
                "person='" + person + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}
