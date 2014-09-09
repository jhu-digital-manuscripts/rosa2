package rosa.archive.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public class ImageList implements HasId, Serializable, Iterable<BookImage> {
    private static final long serialVersionUID = 1L;

    private String id;
    private List<BookImage> images;

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

    public List<BookImage> getImages() {
        return images;
    }

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
        if (!(o instanceof ImageList)) return false;

        ImageList images1 = (ImageList) o;

        if (id != null ? !id.equals(images1.id) : images1.id != null) return false;
        if (images != null ? !images.equals(images1.images) : images1.images != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (images != null ? images.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ImageList{" +
                "id='" + id + '\'' +
                ", images=" + images +
                '}';
    }
}
