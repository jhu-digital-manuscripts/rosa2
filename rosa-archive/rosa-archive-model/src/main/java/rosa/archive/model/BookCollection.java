package rosa.archive.model;

import rosa.archive.model.aor.AorLocation;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * A collection of books stored in the archive.
 */
public class BookCollection implements HasId, Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    /**
     * Array of book IDs for the books in this collection.
     */
    private String[] books;
    private CharacterNames characterNames;
    private IllustrationTitles illustrationTitles;
    private NarrativeSections narrativeSections;
    private SHA1Checksum checksums;
    private BookImage missingImage;
    private HTMLAnnotations htmlAnnos;

    private ReferenceSheet peopleRef;
    private ReferenceSheet locationsRef;
    private BookReferenceSheet booksRef;

    private CollectionMetadata metadata;

    /** Map of IDs to locations in the collection */
    private Map<String, AorLocation> annotationMap;

    /**
     * Create an empty book collection. Is not persisted.
     */
    public BookCollection() {
        books = new String[0];
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return metadata == null ? null : metadata.getLabel();
    }

    /**
     * @return
     *          array containing the IDs of all books in this collection.
     */
    public String[] books() {
        return books;
    }

    public void setBooks(String[] books) {
        this.books = books;
    }

    public ReferenceSheet getPeopleRef() {
        return peopleRef;
    }

    public void setPeopleRef(ReferenceSheet peopleRef) {
        this.peopleRef = peopleRef;
    }

    public ReferenceSheet getLocationsRef() {
        return locationsRef;
    }

    public void setLocationsRef(ReferenceSheet locationsRef) {
        this.locationsRef = locationsRef;
    }

    public BookReferenceSheet getBooksRef() {
        return booksRef;
    }

    public void setBooksRef(BookReferenceSheet booksRef) {
        this.booksRef = booksRef;
    }

    public CharacterNames getCharacterNames() {
        return characterNames;
    }

    public void setCharacterNames(CharacterNames characterNames) {
        this.characterNames = characterNames;
    }

    public IllustrationTitles getIllustrationTitles() {
        return illustrationTitles;
    }

    public void setIllustrationTitles(IllustrationTitles illustrationTitles) {
        this.illustrationTitles = illustrationTitles;
    }

    public NarrativeSections getNarrativeSections() {
        return narrativeSections;
    }

    public void setNarrativeSections(NarrativeSections narrativeSections) {
        this.narrativeSections = narrativeSections;
    }

    public HTMLAnnotations getHTMLAnnotations() {
    	return htmlAnnos;
    }
    
    public void setHTMLAnnotations(HTMLAnnotations annos) {
    	this.htmlAnnos = annos;
    }

    /**
     * Get a list of all languages supported by this collection.
     *
     * @return
     *          List of languages
     */
    public String[] getAllSupportedLanguages() {
        return metadata != null ? metadata.getLanguages() : new String[0];
    }

    /**
     * Checks whether or not a language is supported by this collection.
     *
     * @param language
     *          language code
     * @return
     *          TRUE if language is supported by collection. FALSE otherwise.
     */
    public boolean isLanguageSupported(String language) {
        for (String lang : getAllSupportedLanguages()) {
            if (lang.equals(language)) {
                return true;
            }
        }
        return false;
    }

    public SHA1Checksum getChecksum() {
        return checksums;
    }

    public void setChecksum(SHA1Checksum checksums) {
        this.checksums = checksums;
    }

    public BookImage getMissingImage() {
        return missingImage;
    }

    public void setMissingImage(BookImage missingImage) {
        this.missingImage = missingImage;
    }

    public CollectionMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(CollectionMetadata metadata) {
        this.metadata = metadata;
    }

    public String getDescription() {
        return metadata != null ? metadata.getDescription() : null;
    }

    public String getLogo() {
        return metadata != null ? metadata.getLogoUrl() : null;
    }

    public String[] getChildCollections() {
        return metadata != null ? metadata.getChildren() : new String[0];
    }

    public String[] getParentCollections() {
        return metadata != null ? metadata.getParents() : new String[0];
    }

    public Map<String, AorLocation> getAnnotationMap() {
        return annotationMap;
    }

    public void setAnnotationMap(Map<String, AorLocation> annotationMap) {
        this.annotationMap = annotationMap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookCollection that = (BookCollection) o;
        return Objects.equals(id, that.id) &&
                Arrays.equals(books, that.books) &&
                Objects.equals(characterNames, that.characterNames) &&
                Objects.equals(illustrationTitles, that.illustrationTitles) &&
                Objects.equals(narrativeSections, that.narrativeSections) &&
                Objects.equals(checksums, that.checksums) &&
                Objects.equals(missingImage, that.missingImage) &&
                Objects.equals(peopleRef, that.peopleRef) &&
                Objects.equals(locationsRef, that.locationsRef) &&
                Objects.equals(booksRef, that.booksRef) &&
                Objects.equals(metadata, that.metadata) &&
                Objects.equals(annotationMap, that.annotationMap) &&
                Objects.equals(htmlAnnos, that.htmlAnnos);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, characterNames, illustrationTitles, narrativeSections, checksums, missingImage, peopleRef, locationsRef, booksRef, metadata, annotationMap, htmlAnnos);
        result = 31 * result + Arrays.hashCode(books);
        return result;
    }

    @Override
    public String toString() {
        return "BookCollection{" +
                "id='" + id + '\'' +
                ", books=" + Arrays.toString(books) +
                ", characterNames=" + characterNames +
                ", illustrationTitles=" + illustrationTitles +
                ", narrativeSections=" + narrativeSections +
                ", checksums=" + checksums +
                ", missingImage=" + missingImage +
                ", peopleRef=" + peopleRef +
                ", locationsRef=" + locationsRef +
                ", booksRef=" + booksRef +
                ", metadata=" + metadata +
                ", annotationMap=" + annotationMap +
                '}';
    }
}
