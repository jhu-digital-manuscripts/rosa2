package rosa.archive.model.aor;

import java.io.Serializable;
import java.util.Objects;

public class AorRef implements Serializable {
    private static final long serialVersionUID = 1L;

    private String collection;
    private String book;
    private String page;
    private String annotation;

    public AorRef(String collection, String book, String page, String annotation) {
        this.collection = collection;
        this.book = book;
        this.page = page;
        this.annotation = annotation;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public String getBook() {
        return book;
    }

    public void setBook(String book) {
        this.book = book;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AorRef aorRef = (AorRef) o;
        return Objects.equals(collection, aorRef.collection) &&
                Objects.equals(book, aorRef.book) &&
                Objects.equals(page, aorRef.page) &&
                Objects.equals(annotation, aorRef.annotation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(collection, book, page, annotation);
    }

    @Override
    public String toString() {
        return "AorRef{" +
                "collection='" + collection + '\'' +
                ", book='" + book + '\'' +
                ", page='" + page + '\'' +
                ", annotation='" + annotation + '\'' +
                '}';
    }
}
