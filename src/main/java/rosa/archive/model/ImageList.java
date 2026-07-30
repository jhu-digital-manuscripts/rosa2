package rosa.archive.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * An ordered list of book images, typically representing all page images for a book.
 */
public class ImageList implements HasId, Iterable<BookImage> {

    private String id;
    private List<BookImage> images;

    /**
     * Creates an empty image list.
     */
    public ImageList() {
        this.images = new ArrayList<>();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the list of images.
     *
     * @return the images
     */
    public List<BookImage> getImages() {
        return images;
    }

    /**
     * Sets the list of images.
     *
     * @param images the images to set
     */
    public void setImages(List<BookImage> images) {
        this.images = images;
    }

    @Override
    public Iterator<BookImage> iterator() {
        return images.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImageList that)) return false;
        return Objects.equals(id, that.id) &&
                Objects.equals(images, that.images);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, images);
    }

    @Override
    public String toString() {
        return "ImageList{" +
                "id='" + id + '\'' +
                ", images=" + images +
                '}';
    }
}
