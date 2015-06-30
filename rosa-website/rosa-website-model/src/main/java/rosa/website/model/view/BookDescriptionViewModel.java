package rosa.website.model.view;

import rosa.archive.model.BookDescription;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.ImageList;

import java.io.Serializable;

/**
 * Data model object for the BookDescriptionView.
 */
public class BookDescriptionViewModel implements Serializable {
    private static final long serialVersionUID = 1L;

    private BookDescription prose;
    private BookMetadata metadata;
    private ImageList images;

    public BookDescriptionViewModel() {}

    public BookDescriptionViewModel(BookDescription prose, BookMetadata metadata, ImageList images) {
        this.prose = prose;
        this.metadata = metadata;
        this.images = images;
    }

    public BookDescription getProse() {
        return prose;
    }

    public void setProse(BookDescription prose) {
        this.prose = prose;
    }

    public BookMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(BookMetadata metadata) {
        this.metadata = metadata;
    }

    public ImageList getImages() {
        return images;
    }

    public void setImages(ImageList images) {
        this.images = images;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BookDescriptionViewModel that = (BookDescriptionViewModel) o;

        if (prose != null ? !prose.equals(that.prose) : that.prose != null) return false;
        if (metadata != null ? !metadata.equals(that.metadata) : that.metadata != null) return false;
        return !(images != null ? !images.equals(that.images) : that.images != null);

    }

    @Override
    public int hashCode() {
        int result = prose != null ? prose.hashCode() : 0;
        result = 31 * result + (metadata != null ? metadata.hashCode() : 0);
        result = 31 * result + (images != null ? images.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "BookDescriptionViewModel{" +
                "prose=" + prose +
                ", metadata=" + metadata +
                ", images=" + images +
                '}';
    }
}
