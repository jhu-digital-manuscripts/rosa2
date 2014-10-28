package rosa.archive.model.aor;

import java.io.Serializable;

/**
 *
 */
public class XRef implements Serializable {

    private String person;
    private String title;

    public XRef() {

    }

    public XRef(String person, String title) {
        this.person = person;
        this.title = title;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        XRef xRef = (XRef) o;

        if (person != null ? !person.equals(xRef.person) : xRef.person != null) return false;
        if (title != null ? !title.equals(xRef.title) : xRef.title != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = person != null ? person.hashCode() : 0;
        result = 31 * result + (title != null ? title.hashCode() : 0);
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
