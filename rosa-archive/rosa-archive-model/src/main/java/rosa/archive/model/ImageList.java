package rosa.archive.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public class ImageList implements IsSerializable, Iterable<BookImage> {

    private List<BookImage> images;

    public ImageList() {
        this.images = new ArrayList<>();
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

        ImageList that = (ImageList) o;

        if (images != null ? !images.equals(that.images) : that.images != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return images != null ? images.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "ImageList{" +
                "images=" + images +
                '}';
    }
}
