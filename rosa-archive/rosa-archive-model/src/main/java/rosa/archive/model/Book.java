package rosa.archive.model;

import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.meta.BiblioData;
import rosa.archive.model.meta.MultilangMetadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A single book in the archive.
 */
public class Book implements HasId, Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    /**
     * High resolution images of the pages of this book.
     */
    private ImageList images;
    /**
     * High resolution images of the pages of this book after cropping.
     */
    private ImageList croppedImages;
    /**
     * Information about the cropping of the original images.
     */
    private CropInfo cropInfo;

    private SHA1Checksum SHA1Checksum;
    /**
     * Array of all content associated with this book (ex: all file names in a directory).
     */
    private String[] content;

    private BookStructure bookStructure;
    private IllustrationTagging illustrationTagging;
    private NarrativeTagging manualNarrativeTagging;
    private NarrativeTagging automaticNarrativeTagging;
    private MultilangMetadata multilangMetadata;

    private Map<String, Permission> permissions;
    private Map<String, BookMetadata> metadataMap;
    private Map<String, BookDescription> descriptionMap;
    private Transcription transcription;

    private List<AnnotatedPage> annotatedPages;

    /**
     * Create an empty Book. Not persisted.
     */
    public Book() {
        this.permissions = new HashMap<>();
        this.metadataMap = new HashMap<>();
        this.annotatedPages = new ArrayList<>();
        this.descriptionMap = new HashMap<>();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public ImageList getImages() {
        return images;
    }

    public void setImages(ImageList images) {
        this.images = images;
    }

    public ImageList getCroppedImages() {
        return croppedImages;
    }

    public void setCroppedImages(ImageList croppedImages) {
        this.croppedImages = croppedImages;
    }

    public CropInfo getCropInfo() {
        return cropInfo;
    }

    public void setCropInfo(CropInfo cropInfo) {
        this.cropInfo = cropInfo;
    }

    /**
     * Get book metadata in a particular language.
     *
     * @param language language code
     * @return the book metadata
     */
    public BookMetadata getBookMetadata(String language) {
        if (multilangMetadata != null && multilangMetadata.supportsLanguage(language)) {
            BookMetadata metadata = new BookMetadata();

            metadata.setId(multilangMetadata.getId());
            metadata.setYearStart(multilangMetadata.getYearStart());
            metadata.setYearEnd(multilangMetadata.getYearEnd());
            metadata.setDimensionUnits(multilangMetadata.getDimensionUnits());
            metadata.setDimensions(multilangMetadata.getDimensionsString());
            metadata.setWidth(multilangMetadata.getWidth());
            metadata.setHeight(multilangMetadata.getHeight());
            metadata.setNumberOfPages(multilangMetadata.getNumberOfPages());
            metadata.setNumberOfIllustrations(multilangMetadata.getNumberOfIllustrations());
            metadata.setTexts(multilangMetadata.getBookTexts().toArray(new BookText[0]));

            BiblioData forLang = multilangMetadata.getBiblioDataMap().get(language);
            metadata.setTitle(forLang.getTitle());
            metadata.setDate(forLang.getDateLabel());
            metadata.setCurrentLocation(forLang.getCurrentLocation());
            metadata.setRepository(forLang.getRepository());
            metadata.setShelfmark(forLang.getShelfmark());
            metadata.setOrigin(forLang.getOrigin());
            metadata.setType(forLang.getType());
            metadata.setCommonName(forLang.getCommonName());
            metadata.setMaterial(forLang.getMaterial());

            return metadata;
        }
        return metadataMap.get(language);
    }

    /**
     * Add book metadata in a specific language.
     *
     * @param metadata metadata to add
     * @param language language code
     */
    public void addBookMetadata(BookMetadata metadata, String language) {
        metadataMap.put(language, metadata);
    }

    public void setBookMetadata(Map<String, BookMetadata> metadataMap) {
        this.metadataMap = metadataMap;
    }

    public void addBookDescription(BookDescription description, String language) {
        descriptionMap.put(language, description);
    }

    public BookDescription getBookDescription(String lang) {
        return descriptionMap.get(lang);
    }

    public SHA1Checksum getChecksum() {
        return SHA1Checksum;
    }

    public void setChecksum(SHA1Checksum SHA1Checksum) {
        this.SHA1Checksum = SHA1Checksum;
    }

    public String[] getContent() {
        return content;
    }

    /**
     * Sorts content array before setting the field.
     *
     * @param content array of content
     */
    public void setContent(String[] content) {
        Arrays.sort(content);
        this.content = content;
    }

    public BookStructure getBookStructure() {
        return bookStructure;
    }

    public void setBookStructure(BookStructure bookStructure) {
        this.bookStructure = bookStructure;
    }

    public IllustrationTagging getIllustrationTagging() {
        return illustrationTagging;
    }

    public void setIllustrationTagging(IllustrationTagging illustrationTagging) {
        this.illustrationTagging = illustrationTagging;
    }

    public NarrativeTagging getManualNarrativeTagging() {
        return manualNarrativeTagging;
    }

    public void setManualNarrativeTagging(NarrativeTagging manualNarrativeTagging) {
        this.manualNarrativeTagging = manualNarrativeTagging;
    }

    public NarrativeTagging getAutomaticNarrativeTagging() {
        return automaticNarrativeTagging;
    }

    public void setAutomaticNarrativeTagging(NarrativeTagging automaticNarrativeTagging) {
        this.automaticNarrativeTagging = automaticNarrativeTagging;
    }

    /**
     * Add permission statement in a particular language.
     *
     * @param permission permission
     * @param language langauge code
     */
    public void addPermission(Permission permission, String language) {
        permissions.put(language, permission);
    }

    /**
     * Get the permission statement in a particular language.
     *
     * @param language language code
     * @return the permission statement
     */
    public Permission getPermission(String language) {
        return permissions.get(language);
    }

    public Permission[] getPermissionsInAllLanguages() {
        List<Permission> perms = new ArrayList<>();

        for (String lang : permissions.keySet()) {
            perms.add(permissions.get(lang));
        }

        return perms.toArray(new Permission[perms.size()]);
    }

    public Transcription getTranscription() {
        return transcription;
    }

    public void setTranscription(Transcription transcription) {
        this.transcription = transcription;
    }

    public List<AnnotatedPage> getAnnotatedPages() {
        return annotatedPages;
    }

    /**
     * Get AoR transcriptions for a particular page.
     *
     * @param page the page
     * @return AoR transcription
     */
    public AnnotatedPage getAnnotationPage(String page) {
        for (AnnotatedPage ap : annotatedPages) {
            if (ap.getPage().contains(page)) {
                return ap;
            }
        }
        return null;
    }

    public void setAnnotatedPages(List<AnnotatedPage> annotatedPages) {
        this.annotatedPages = annotatedPages;
    }

    public void setMultilangMetadata(MultilangMetadata multilangMetadata) {
        this.multilangMetadata = multilangMetadata;
    }

    
    /**
     * Guess name of image from fragment.
     * 
     * @param frag
     * @return image id or null
     */
    public String guessImageName(String frag) {
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
            
            for (BookImage image: images) {
                if (image.getId().equalsIgnoreCase(frag)) {
                    return image.getId();
                }
            }

            return null;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Book book = (Book) o;

        if (id != null ? !id.equals(book.id) : book.id != null) return false;
        if (images != null ? !images.equals(book.images) : book.images != null) return false;
        if (croppedImages != null ? !croppedImages.equals(book.croppedImages) : book.croppedImages != null)
            return false;
        if (cropInfo != null ? !cropInfo.equals(book.cropInfo) : book.cropInfo != null) return false;
        if (SHA1Checksum != null ? !SHA1Checksum.equals(book.SHA1Checksum) : book.SHA1Checksum != null) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(content, book.content)) return false;
        if (bookStructure != null ? !bookStructure.equals(book.bookStructure) : book.bookStructure != null)
            return false;
        if (illustrationTagging != null ? !illustrationTagging.equals(book.illustrationTagging) : book.illustrationTagging != null)
            return false;
        if (manualNarrativeTagging != null ? !manualNarrativeTagging.equals(book.manualNarrativeTagging) : book.manualNarrativeTagging != null)
            return false;
        if (automaticNarrativeTagging != null ? !automaticNarrativeTagging.equals(book.automaticNarrativeTagging) : book.automaticNarrativeTagging != null)
            return false;
        if (multilangMetadata != null ? !multilangMetadata.equals(book.multilangMetadata) : book.multilangMetadata != null)
            return false;
        if (permissions != null ? !permissions.equals(book.permissions) : book.permissions != null) return false;
        if (metadataMap != null ? !metadataMap.equals(book.metadataMap) : book.metadataMap != null) return false;
        if (descriptionMap != null ? !descriptionMap.equals(book.descriptionMap) : book.descriptionMap != null)
            return false;
        if (transcription != null ? !transcription.equals(book.transcription) : book.transcription != null)
            return false;
        return !(annotatedPages != null ? !annotatedPages.equals(book.annotatedPages) : book.annotatedPages != null);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (images != null ? images.hashCode() : 0);
        result = 31 * result + (croppedImages != null ? croppedImages.hashCode() : 0);
        result = 31 * result + (cropInfo != null ? cropInfo.hashCode() : 0);
        result = 31 * result + (SHA1Checksum != null ? SHA1Checksum.hashCode() : 0);
        result = 31 * result + (content != null ? Arrays.hashCode(content) : 0);
        result = 31 * result + (bookStructure != null ? bookStructure.hashCode() : 0);
        result = 31 * result + (illustrationTagging != null ? illustrationTagging.hashCode() : 0);
        result = 31 * result + (manualNarrativeTagging != null ? manualNarrativeTagging.hashCode() : 0);
        result = 31 * result + (automaticNarrativeTagging != null ? automaticNarrativeTagging.hashCode() : 0);
        result = 31 * result + (multilangMetadata != null ? multilangMetadata.hashCode() : 0);
        result = 31 * result + (permissions != null ? permissions.hashCode() : 0);
        result = 31 * result + (metadataMap != null ? metadataMap.hashCode() : 0);
        result = 31 * result + (descriptionMap != null ? descriptionMap.hashCode() : 0);
        result = 31 * result + (transcription != null ? transcription.hashCode() : 0);
        result = 31 * result + (annotatedPages != null ? annotatedPages.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Book{" +
                "id='" + id + '\'' +
                ", images=" + images +
                ", croppedImages=" + croppedImages +
                ", cropInfo=" + cropInfo +
                ", SHA1Checksum=" + SHA1Checksum +
                ", content=" + Arrays.toString(content) +
                ", bookStructure=" + bookStructure +
                ", illustrationTagging=" + illustrationTagging +
                ", manualNarrativeTagging=" + manualNarrativeTagging +
                ", automaticNarrativeTagging=" + automaticNarrativeTagging +
                ", multilangMetadata=" + multilangMetadata +
                ", permissions=" + permissions +
                ", metadataMap=" + metadataMap +
                ", descriptionMap=" + descriptionMap +
                ", transcription=" + transcription +
                ", annotatedPages=" + annotatedPages +
                '}';
    }
}
