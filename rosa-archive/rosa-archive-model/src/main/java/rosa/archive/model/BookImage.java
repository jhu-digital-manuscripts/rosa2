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

    /**
     * Create an empty BookImage
     */
    public BookImage() {
        this(null, -1, -1, true);
    }

    /**
     * Create a BookImage with parameters.
     *
     * @param id ID
     * @param width width in pixels
     * @param height height in pixels
     * @param isMissing is this image missing from the archive?
     */
    public BookImage(String id, int width, int height, boolean isMissing) {
        this.id = id;
        this.width = width;
        this.height = height;
        this.isMissing = isMissing;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * TODO can be abstracted
     * @return the page this image is associated with
     */
    public String getPage() {
        String[] parts = id.split("\\.");
        StringBuilder sb = new StringBuilder();
        // Strip off book ID and file extension to get page name
        for (int i = 1; i < (parts.length - 1); i++) {
            sb.append(parts[i]);
        }

        return sb.toString();
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
