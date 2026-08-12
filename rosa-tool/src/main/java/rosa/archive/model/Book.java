package rosa.archive.model;

import rosa.archive.model.aor.AnnotatedPage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A single book in the archive. Contains page images, metadata, annotations,
 * transcriptions, and structural information.
 */
public class Book implements HasId {

    private String id;
    private ImageList images;
    private ImageList croppedImages;
    private CropInfo cropInfo;
    private SHA1Checksum checksum;
    private String[] content;
    private BookStructure bookStructure;
    private IllustrationTagging illustrationTagging;
    private NarrativeTagging manualNarrativeTagging;
    private NarrativeTagging automaticNarrativeTagging;
    private BookMetadata bookMetadata;
    private Map<String, Permission> permissions;
    private Map<String, BookDescription> descriptions;
    private Transcription transcription;
    private List<AnnotatedPage> annotatedPages;

    /**
     * Creates an empty Book.
     */
    public Book() {
        this.permissions = new HashMap<>();
        this.descriptions = new HashMap<>();
        this.annotatedPages = new ArrayList<>();
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
     * Returns the high-resolution page images for this book.
     *
     * @return the image list, or {@code null}
     */
    public ImageList getImages() {
        return images;
    }

    /**
     * Sets the high-resolution page images.
     *
     * @param images the image list
     */
    public void setImages(ImageList images) {
        this.images = images;
    }

    /**
     * Returns the cropped page images for this book.
     *
     * @return the cropped image list, or {@code null}
     */
    public ImageList getCroppedImages() {
        return croppedImages;
    }

    /**
     * Sets the cropped page images.
     *
     * @param croppedImages the cropped image list
     */
    public void setCroppedImages(ImageList croppedImages) {
        this.croppedImages = croppedImages;
    }

    /**
     * Returns the crop information for images.
     *
     * @return the crop info, or {@code null}
     */
    public CropInfo getCropInfo() {
        return cropInfo;
    }

    /**
     * Sets the crop information.
     *
     * @param cropInfo the crop info
     */
    public void setCropInfo(CropInfo cropInfo) {
        this.cropInfo = cropInfo;
    }

    /**
     * Returns the license URL from book metadata.
     *
     * @return the license URL, or {@code null}
     */
    public String getLicenseUrl() {
        return bookMetadata != null ? bookMetadata.getLicenseUrl() : null;
    }

    /**
     * Returns the license logo URL from book metadata.
     *
     * @return the license logo URL, or {@code null}
     */
    public String getLicenseLogoUrl() {
        return bookMetadata != null ? bookMetadata.getLicenseLogo() : null;
    }

    /**
     * Returns the book metadata.
     *
     * @return the metadata, or {@code null}
     */
    public BookMetadata getBookMetadata() {
        return bookMetadata;
    }

    /**
     * Sets the book metadata.
     *
     * @param bookMetadata the metadata
     */
    public void setBookMetadata(BookMetadata bookMetadata) {
        this.bookMetadata = bookMetadata;
    }

    /**
     * Returns bibliographic data in the given language.
     *
     * @param languageCode the language code
     * @return the bibliographic data, or {@code null} if not available
     */
    public BiblioData getBiblioData(String languageCode) {
        return bookMetadata != null ? bookMetadata.getBiblioDataMap().get(languageCode) : null;
    }

    /**
     * Returns the SHA-1 checksum data for this book.
     *
     * @return the checksum, or {@code null}
     */
    public SHA1Checksum getChecksum() {
        return checksum;
    }

    /**
     * Sets the SHA-1 checksum data.
     *
     * @param checksum the checksum
     */
    public void setChecksum(SHA1Checksum checksum) {
        this.checksum = checksum;
    }

    /**
     * Returns the array of all content filenames in this book's directory.
     *
     * @return the content array, or {@code null}
     */
    public String[] getContent() {
        return content;
    }

    /**
     * Sets the content array, sorting it alphabetically.
     *
     * @param content the content filenames
     */
    public void setContent(String[] content) {
        Arrays.sort(content);
        this.content = content;
    }

    /**
     * Returns the book structure (page/column layout).
     *
     * @return the book structure, or {@code null}
     */
    public BookStructure getBookStructure() {
        return bookStructure;
    }

    /**
     * Sets the book structure.
     *
     * @param bookStructure the book structure
     */
    public void setBookStructure(BookStructure bookStructure) {
        this.bookStructure = bookStructure;
    }

    /**
     * Returns the illustration tagging data.
     *
     * @return the illustration tagging, or {@code null}
     */
    public IllustrationTagging getIllustrationTagging() {
        return illustrationTagging;
    }

    /**
     * Sets the illustration tagging data.
     *
     * @param illustrationTagging the illustration tagging
     */
    public void setIllustrationTagging(IllustrationTagging illustrationTagging) {
        this.illustrationTagging = illustrationTagging;
    }

    /**
     * Checks whether illustration tagging exists for the specified page.
     *
     * @param page the page short name
     * @return {@code true} if illustration tagging exists for the page
     */
    public boolean hasIllustrationTagging(String page) {
        if (illustrationTagging != null) {
            for (int i = 0; i < illustrationTagging.size(); i++) {
                if (illustrationTagging.getIllustrationData(i).getPage().equals(page)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the manual narrative tagging data.
     *
     * @return the manual narrative tagging, or {@code null}
     */
    public NarrativeTagging getManualNarrativeTagging() {
        return manualNarrativeTagging;
    }

    /**
     * Sets the manual narrative tagging data.
     *
     * @param manualNarrativeTagging the manual narrative tagging
     */
    public void setManualNarrativeTagging(NarrativeTagging manualNarrativeTagging) {
        this.manualNarrativeTagging = manualNarrativeTagging;
    }

    /**
     * Checks whether narrative tagging covers the specified page.
     *
     * @param page the page identifier
     * @return {@code true} if narrative tagging exists for the page
     */
    public boolean hasNarrativeTagging(String page) {
        if (manualNarrativeTagging != null) {
            for (BookScene scene : manualNarrativeTagging.getScenes()) {
                if (scene.getStartPage().compareToIgnoreCase(page) <= 0 &&
                        scene.getEndPage().compareToIgnoreCase(page) >= 0) {
                    return true;
                }
            }
        } else if (automaticNarrativeTagging != null) {
            for (BookScene scene : automaticNarrativeTagging.getScenes()) {
                if (scene.getStartPage().compareToIgnoreCase(page) <= 0 &&
                        scene.getEndPage().compareToIgnoreCase(page) >= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the automatic narrative tagging data.
     *
     * @return the automatic narrative tagging, or {@code null}
     */
    public NarrativeTagging getAutomaticNarrativeTagging() {
        return automaticNarrativeTagging;
    }

    /**
     * Sets the automatic narrative tagging data.
     *
     * @param automaticNarrativeTagging the automatic narrative tagging
     */
    public void setAutomaticNarrativeTagging(NarrativeTagging automaticNarrativeTagging) {
        this.automaticNarrativeTagging = automaticNarrativeTagging;
    }

    /**
     * Adds a permission statement in a particular language.
     *
     * @param permission the permission
     * @param language   the language code
     */
    public void addPermission(Permission permission, String language) {
        permissions.put(language, permission);
    }

    /**
     * Returns the permission statement in the specified language.
     *
     * @param language the language code
     * @return the permission, or {@code null} if not available
     */
    public Permission getPermission(String language) {
        return permissions.get(language);
    }

    /**
     * Returns all permission statements across all languages.
     *
     * @return array of all permissions
     */
    public Permission[] getPermissionsInAllLanguages() {
        return permissions.values().toArray(new Permission[0]);
    }

    /**
     * Adds a description in a particular language.
     *
     * @param description the description
     * @param language    the language code
     */
    public void addDescription(BookDescription description, String language) {
        descriptions.put(language, description);
    }

    /**
     * Returns the description in the specified language.
     *
     * @param language the language code
     * @return the description, or {@code null} if not available
     */
    public BookDescription getDescription(String language) {
        return descriptions.get(language);
    }

    /**
     * Returns all descriptions across all languages.
     *
     * @return array of all descriptions
     */
    public BookDescription[] getDescriptionsInAllLanguages() {
        return descriptions.values().toArray(new BookDescription[0]);
    }

    /**
     * Returns the transcription data for this book.
     *
     * @return the transcription, or {@code null}
     */
    public Transcription getTranscription() {
        return transcription;
    }

    /**
     * Sets the transcription data.
     *
     * @param transcription the transcription
     */
    public void setTranscription(Transcription transcription) {
        this.transcription = transcription;
    }

    /**
     * Returns all annotated pages for this book.
     *
     * @return the annotated pages list
     */
    public List<AnnotatedPage> getAnnotatedPages() {
        return annotatedPages;
    }

    /**
     * Returns the AoR annotated page matching the given page identifier.
     *
     * @param page the page name to search for
     * @return the annotated page, or {@code null} if not found
     */
    public AnnotatedPage getAnnotationPage(String page) {
        for (AnnotatedPage ap : annotatedPages) {
            if (ap.getPage() != null && ap.getPage().contains(page)) {
                return ap;
            }
        }
        return null;
    }

    /**
     * Sets the annotated pages list.
     *
     * @param annotatedPages the annotated pages
     */
    public void setAnnotatedPages(List<AnnotatedPage> annotatedPages) {
        this.annotatedPages = annotatedPages;
    }

    /**
     * Guesses the full image object from a name fragment, applying common naming
     * conventions (adding recto suffix, zero-padding, adding .tif extension, adding book id prefix).
     *
     * @param frag the name fragment
     * @return the matching BookImage, or {@code null} if not found
     */
    public BookImage guessImage(String frag) {
        frag = frag.trim();

        if (frag.matches("\\d+")) {
            frag += "r";
        }

        if (frag.matches("\\d[rRvV]")) {
            frag = "00" + frag;
        } else if (frag.matches("\\d\\d[rRvV]")) {
            frag = "0" + frag;
        }

        if (!frag.endsWith(".tif")) {
            frag += ".tif";
        }

        if (!frag.startsWith(id)) {
            frag = id + "." + frag;
        }

        if (images == null) {
            return null;
        }

        for (BookImage image : images) {
            if (image.getId().equalsIgnoreCase(frag)) {
                return image;
            }
        }
        return null;
    }

    /**
     * Guesses the full image name from a name fragment.
     *
     * @param frag the name fragment
     * @return the image id, or {@code null} if not found
     */
    public String guessImageName(String frag) {
        BookImage result = guessImage(frag);
        return result == null ? null : result.getId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Book that)) return false;
        return Objects.equals(id, that.id) &&
                Objects.equals(images, that.images) &&
                Objects.equals(croppedImages, that.croppedImages) &&
                Objects.equals(cropInfo, that.cropInfo) &&
                Objects.equals(checksum, that.checksum) &&
                Arrays.equals(content, that.content) &&
                Objects.equals(bookStructure, that.bookStructure) &&
                Objects.equals(illustrationTagging, that.illustrationTagging) &&
                Objects.equals(manualNarrativeTagging, that.manualNarrativeTagging) &&
                Objects.equals(automaticNarrativeTagging, that.automaticNarrativeTagging) &&
                Objects.equals(bookMetadata, that.bookMetadata) &&
                Objects.equals(permissions, that.permissions) &&
                Objects.equals(descriptions, that.descriptions) &&
                Objects.equals(transcription, that.transcription) &&
                Objects.equals(annotatedPages, that.annotatedPages);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, images, croppedImages, cropInfo, checksum,
                bookStructure, illustrationTagging, manualNarrativeTagging,
                automaticNarrativeTagging, bookMetadata, permissions, descriptions,
                transcription, annotatedPages);
        result = 31 * result + Arrays.hashCode(content);
        return result;
    }

    @Override
    public String toString() {
        return "Book{" +
                "id='" + id + '\'' +
                ", images=" + images +
                ", croppedImages=" + croppedImages +
                ", cropInfo=" + cropInfo +
                ", checksum=" + checksum +
                ", content=" + Arrays.toString(content) +
                ", bookStructure=" + bookStructure +
                ", illustrationTagging=" + illustrationTagging +
                ", manualNarrativeTagging=" + manualNarrativeTagging +
                ", automaticNarrativeTagging=" + automaticNarrativeTagging +
                ", bookMetadata=" + bookMetadata +
                ", permissions=" + permissions +
                ", descriptions=" + descriptions +
                ", transcription=" + transcription +
                ", annotatedPages=" + annotatedPages +
                '}';
    }
}
