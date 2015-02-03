package rosa.archive.model;

import java.io.Serializable;
import java.util.List;

public final class BookReferenceSheet extends ReferenceSheet implements HasId, Serializable {
    private static final long serialVersionUID = 1L;

    private List<String> authors;
    private String fullTitle;

    public BookReferenceSheet() {
        super();
    }

    public List<String> getAuthors() {
        return authors;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors;
    }

    public String getFullTitle() {
        return fullTitle;
    }

    public void setFullTitle(String fullTitle) {
        this.fullTitle = fullTitle;
    }

    @Override
    public boolean canEqual(Object o) {
        return (o instanceof BookReferenceSheet);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookReferenceSheet)) return false;
        if (!super.equals(o)) return false;

        BookReferenceSheet that = (BookReferenceSheet) o;
        if (!that.canEqual(this)) return false;

        if (authors != null ? !authors.equals(that.authors) : that.authors != null) return false;
        if (fullTitle != null ? !fullTitle.equals(that.fullTitle) : that.fullTitle != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (authors != null ? authors.hashCode() : 0);
        result = 31 * result + (fullTitle != null ? fullTitle.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "BookReferenceSheet{" +
                super.toString() +
                "authors=" + authors +
                ", fullTitle='" + fullTitle + '\'' +
                '}';
    }
}
