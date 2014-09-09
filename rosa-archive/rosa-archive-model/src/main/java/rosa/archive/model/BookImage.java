package rosa.archive.model;

import java.io.Serializable;

/**
 * A high resolution image of a page in a book. Dimensions should be in pixels!
 */
public class BookImage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private int width;
    private int height;
    private boolean isMissing;

    public BookImage() {
        width = -1;
        height = -1;
        isMissing = true;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean isMissing() {
        return isMissing;
    }

    public void setMissing(boolean isMissing) {
        this.isMissing = isMissing;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookImage)) return false;

        BookImage bookImage = (BookImage) o;

        if (height != bookImage.height) return false;
        if (isMissing != bookImage.isMissing) return false;
        if (width != bookImage.width) return false;
        if (id != null ? !id.equals(bookImage.id) : bookImage.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + width;
        result = 31 * result + height;
        result = 31 * result + (isMissing ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "BookImage{" +
                "id='" + id + '\'' +
                ", width=" + width +
                ", height=" + height +
                ", isMissing=" + isMissing +
                '}';
    }
}
