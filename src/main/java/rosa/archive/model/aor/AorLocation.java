package rosa.archive.model.aor;

import java.util.Objects;

/**
 * Represents the location of an annotation within the archive hierarchy:
 * collection, book, page, and annotation identifier.
 */
public class AorLocation {

    private String collection;
    private String book;
    private String page;
    private String annotation;

    /**
     * Creates an AorLocation with the specified coordinates.
     *
     * @param collection the collection identifier
     * @param book       the book identifier
     * @param page       the page identifier
     * @param annotation the annotation identifier
     */
    public AorLocation(String collection, String book, String page, String annotation) {
        this.collection = collection;
        this.book = book;
        this.page = page;
        this.annotation = annotation;
    }

    /**
     * Returns the collection identifier.
     *
     * @return the collection id
     */
    public String getCollection() {
        return collection;
    }

    /**
     * Sets the collection identifier.
     *
     * @param collection the collection id
     */
    public void setCollection(String collection) {
        this.collection = collection;
    }

    /**
     * Returns the book identifier.
     *
     * @return the book id
     */
    public String getBook() {
        return book;
    }

    /**
     * Sets the book identifier.
     *
     * @param book the book id
     */
    public void setBook(String book) {
        this.book = book;
    }

    /**
     * Returns the page identifier.
     *
     * @return the page id
     */
    public String getPage() {
        return page;
    }

    /**
     * Sets the page identifier.
     *
     * @param page the page id
     */
    public void setPage(String page) {
        this.page = page;
    }

    /**
     * Returns the annotation identifier.
     *
     * @return the annotation id
     */
    public String getAnnotation() {
        return annotation;
    }

    /**
     * Sets the annotation identifier.
     *
     * @param annotation the annotation id
     */
    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AorLocation that)) return false;
        return Objects.equals(collection, that.collection) &&
                Objects.equals(book, that.book) &&
                Objects.equals(page, that.page) &&
                Objects.equals(annotation, that.annotation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(collection, book, page, annotation);
    }

    @Override
    public String toString() {
        return "AorLocation{" +
                "collection='" + collection + '\'' +
                ", book='" + book + '\'' +
                ", page='" + page + '\'' +
                ", annotation='" + annotation + '\'' +
                '}';
    }
}
