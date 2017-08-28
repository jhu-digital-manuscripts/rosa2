package rosa.archive.model;

import java.io.Serializable;
import java.util.Arrays;

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

    private ReferenceSheet peopleRef;
    private ReferenceSheet locationsRef;
    private BookReferenceSheet booksRef;

    private CollectionMetadata metadata;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BookCollection that = (BookCollection) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (!Arrays.equals(books, that.books)) return false;
        if (characterNames != null ? !characterNames.equals(that.characterNames) : that.characterNames != null)
            return false;
        if (illustrationTitles != null ? !illustrationTitles.equals(that.illustrationTitles) : that.illustrationTitles != null)
            return false;
        if (narrativeSections != null ? !narrativeSections.equals(that.narrativeSections) : that.narrativeSections != null)
            return false;
        if (checksums != null ? !checksums.equals(that.checksums) : that.checksums != null) return false;
        if (missingImage != null ? !missingImage.equals(that.missingImage) : that.missingImage != null) return false;
        if (peopleRef != null ? !peopleRef.equals(that.peopleRef) : that.peopleRef != null) return false;
        if (locationsRef != null ? !locationsRef.equals(that.locationsRef) : that.locationsRef != null) return false;
        if (booksRef != null ? !booksRef.equals(that.booksRef) : that.booksRef != null) return false;
        return metadata != null ? metadata.equals(that.metadata) : that.metadata == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + Arrays.hashCode(books);
        result = 31 * result + (characterNames != null ? characterNames.hashCode() : 0);
        result = 31 * result + (illustrationTitles != null ? illustrationTitles.hashCode() : 0);
        result = 31 * result + (narrativeSections != null ? narrativeSections.hashCode() : 0);
        result = 31 * result + (checksums != null ? checksums.hashCode() : 0);
        result = 31 * result + (missingImage != null ? missingImage.hashCode() : 0);
        result = 31 * result + (peopleRef != null ? peopleRef.hashCode() : 0);
        result = 31 * result + (locationsRef != null ? locationsRef.hashCode() : 0);
        result = 31 * result + (booksRef != null ? booksRef.hashCode() : 0);
        result = 31 * result + (metadata != null ? metadata.hashCode() : 0);
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
                '}';
    }
}
