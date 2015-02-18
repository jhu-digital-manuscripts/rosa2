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

    private String name;
    private BookImageLocation location;
    private BookImageRole role;
//    private char side; TODO if needed

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BookImageLocation getLocation() {
        return location;
    }

    public void setLocation(BookImageLocation location) {
        this.location = location;
    }

    public BookImageRole getRole() {
        return role;
    }

    public void setRole(BookImageRole role) {
        this.role = role;
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

        BookImage image = (BookImage) o;

        if (height != image.height) return false;
        if (isMissing != image.isMissing) return false;
        if (width != image.width) return false;
        if (id != null ? !id.equals(image.id) : image.id != null) return false;
        if (location != image.location) return false;
        if (name != null ? !name.equals(image.name) : image.name != null) return false;
        if (role != image.role) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + width;
        result = 31 * result + height;
        result = 31 * result + (isMissing ? 1 : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (location != null ? location.hashCode() : 0);
        result = 31 * result + (role != null ? role.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "BookImage{" +
                "id='" + id + '\'' +
                ", width=" + width +
                ", height=" + height +
                ", isMissing=" + isMissing +
                ", name='" + name + '\'' +
                ", location=" + location +
                ", role=" + role +
                '}';
    }
}
