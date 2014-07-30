package rosa.archive.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Collection;
import java.util.List;

/**
 * A single book in the archive.
 */
public class Book implements IsSerializable {

    private String id;
    /**
     * High resolution images of the pages of this book.
     */
    private Collection<BookImage> images;
    /**
     * High resolution images of the pages of this book after cropping.
     */
    private Collection<BookImage> croppedImages;
    /**
     * Information about the cropping of the original images.
     */
    private CropInfo cropInfo;
    private BookMetadata bookMetadata;
    private BookDescription bookDescription;
    private ChecksumInfo checksumInfo;
    /**
     * Collection of all content associated with this book (ex: all file names in a directory).
     */
    private Collection<String> content;

    private BookStructure bookStructure;
    private ImageTagging imageTagging;
    private List<BookScene> manualNarrativeTagging;
    private List<BookScene> automaticNarrativeTagging;

    public Book() {  }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Collection<BookImage> getImages() {
        return images;
    }

    public void setImages(Collection<BookImage> images) {
        this.images = images;
    }

    public Collection<BookImage> getCroppedImages() {
        return croppedImages;
    }

    public void setCroppedImages(Collection<BookImage> croppedImages) {
        this.croppedImages = croppedImages;
    }

    public CropInfo getCropInfo() {
        return cropInfo;
    }

    public void setCropInfo(CropInfo cropInfo) {
        this.cropInfo = cropInfo;
    }

    public BookMetadata getBookMetadata() {
        return bookMetadata;
    }

    public void setBookMetadata(BookMetadata bookMetadata) {
        this.bookMetadata = bookMetadata;
    }

    public BookDescription getBookDescription() {
        return bookDescription;
    }

    public void setBookDescription(BookDescription bookDescription) {
        this.bookDescription = bookDescription;
    }

    public ChecksumInfo getChecksumInfo() {
        return checksumInfo;
    }

    public void setChecksumInfo(ChecksumInfo checksumInfo) {
        this.checksumInfo = checksumInfo;
    }

    public Collection<String> getContent() {
        return content;
    }

    public void setContent(Collection<String> content) {
        this.content = content;
    }

    public BookStructure getBookStructure() {
        return bookStructure;
    }

    public void setBookStructure(BookStructure bookStructure) {
        this.bookStructure = bookStructure;
    }

    public ImageTagging getImageTagging() {
        return imageTagging;
    }

    public void setImageTagging(ImageTagging imageTagging) {
        this.imageTagging = imageTagging;
    }

    public List<BookScene> getManualNarrativeTagging() {
        return manualNarrativeTagging;
    }

    public void setManualNarrativeTagging(List<BookScene> manualNarrativeTagging) {
        this.manualNarrativeTagging = manualNarrativeTagging;
    }

    public List<BookScene> getAutomaticNarrativeTagging() {
        return automaticNarrativeTagging;
    }

    public void setAutomaticNarrativeTagging(List<BookScene> automaticNarrativeTagging) {
        this.automaticNarrativeTagging = automaticNarrativeTagging;
    }

    // TODO equals/hashCode (which fields can be null?

}
