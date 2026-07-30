package rosa.archive.model;

import rosa.archive.model.aor.AorLocation;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * A collection of books stored in the archive. Contains collection-level
 * metadata, reference sheets for people/locations/books, and shared data
 * like character names, illustration titles, and narrative sections.
 */
public class BookCollection implements HasId {

    private String id;
    private String[] books;
    private CharacterNames characterNames;
    private IllustrationTitles illustrationTitles;
    private NarrativeSections narrativeSections;
    private SHA1Checksum checksums;
    private BookImage missingImage;
    private HTMLAnnotations htmlAnnotations;
    private ReferenceSheet peopleRef;
    private ReferenceSheet locationsRef;
    private BookReferenceSheet booksRef;
    private CollectionMetadata metadata;
    private Map<String, AorLocation> annotationMap;

    /**
     * Creates an empty BookCollection.
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

    /**
     * Returns the display label for this collection from its metadata.
     *
     * @return the label, or {@code null} if metadata is not set
     */
    public String getLabel() {
        return metadata == null ? null : metadata.getLabel();
    }

    /**
     * Returns the array of book identifiers in this collection.
     *
     * @return the book ids array
     */
    public String[] books() {
        return books;
    }

    /**
     * Sets the array of book identifiers.
     *
     * @param books the book ids
     */
    public void setBooks(String[] books) {
        this.books = books;
    }

    /**
     * Returns the people reference sheet.
     *
     * @return the people reference sheet, or {@code null}
     */
    public ReferenceSheet getPeopleRef() {
        return peopleRef;
    }

    /**
     * Sets the people reference sheet.
     *
     * @param peopleRef the people reference sheet
     */
    public void setPeopleRef(ReferenceSheet peopleRef) {
        this.peopleRef = peopleRef;
    }

    /**
     * Returns the locations reference sheet.
     *
     * @return the locations reference sheet, or {@code null}
     */
    public ReferenceSheet getLocationsRef() {
        return locationsRef;
    }

    /**
     * Sets the locations reference sheet.
     *
     * @param locationsRef the locations reference sheet
     */
    public void setLocationsRef(ReferenceSheet locationsRef) {
        this.locationsRef = locationsRef;
    }

    /**
     * Returns the books reference sheet.
     *
     * @return the books reference sheet, or {@code null}
     */
    public BookReferenceSheet getBooksRef() {
        return booksRef;
    }

    /**
     * Sets the books reference sheet.
     *
     * @param booksRef the books reference sheet
     */
    public void setBooksRef(BookReferenceSheet booksRef) {
        this.booksRef = booksRef;
    }

    /**
     * Returns the character names data for this collection.
     *
     * @return the character names, or {@code null}
     */
    public CharacterNames getCharacterNames() {
        return characterNames;
    }

    /**
     * Sets the character names data.
     *
     * @param characterNames the character names
     */
    public void setCharacterNames(CharacterNames characterNames) {
        this.characterNames = characterNames;
    }

    /**
     * Returns the illustration titles data for this collection.
     *
     * @return the illustration titles, or {@code null}
     */
    public IllustrationTitles getIllustrationTitles() {
        return illustrationTitles;
    }

    /**
     * Sets the illustration titles data.
     *
     * @param illustrationTitles the illustration titles
     */
    public void setIllustrationTitles(IllustrationTitles illustrationTitles) {
        this.illustrationTitles = illustrationTitles;
    }

    /**
     * Returns the narrative sections data for this collection.
     *
     * @return the narrative sections, or {@code null}
     */
    public NarrativeSections getNarrativeSections() {
        return narrativeSections;
    }

    /**
     * Sets the narrative sections data.
     *
     * @param narrativeSections the narrative sections
     */
    public void setNarrativeSections(NarrativeSections narrativeSections) {
        this.narrativeSections = narrativeSections;
    }

    /**
     * Returns the HTML annotations for this collection.
     *
     * @return the HTML annotations, or {@code null}
     */
    public HTMLAnnotations getHTMLAnnotations() {
        return htmlAnnotations;
    }

    /**
     * Sets the HTML annotations.
     *
     * @param htmlAnnotations the HTML annotations
     */
    public void setHTMLAnnotations(HTMLAnnotations htmlAnnotations) {
        this.htmlAnnotations = htmlAnnotations;
    }

    /**
     * Returns all supported language codes for this collection.
     *
     * @return the language codes array (empty if metadata is not set)
     */
    public String[] getAllSupportedLanguages() {
        return metadata != null ? metadata.getLanguages() : new String[0];
    }

    /**
     * Checks whether the given language is supported by this collection.
     *
     * @param language the language code
     * @return {@code true} if the language is supported
     */
    public boolean isLanguageSupported(String language) {
        for (String lang : getAllSupportedLanguages()) {
            if (lang.equals(language)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the SHA-1 checksum data for this collection.
     *
     * @return the checksums, or {@code null}
     */
    public SHA1Checksum getChecksum() {
        return checksums;
    }

    /**
     * Sets the SHA-1 checksum data.
     *
     * @param checksums the checksums
     */
    public void setChecksum(SHA1Checksum checksums) {
        this.checksums = checksums;
    }

    /**
     * Returns the placeholder image used when a page image is missing.
     *
     * @return the missing image, or {@code null}
     */
    public BookImage getMissingImage() {
        return missingImage;
    }

    /**
     * Sets the placeholder missing image.
     *
     * @param missingImage the missing image
     */
    public void setMissingImage(BookImage missingImage) {
        this.missingImage = missingImage;
    }

    /**
     * Returns the collection metadata.
     *
     * @return the metadata, or {@code null}
     */
    public CollectionMetadata getMetadata() {
        return metadata;
    }

    /**
     * Sets the collection metadata.
     *
     * @param metadata the metadata
     */
    public void setMetadata(CollectionMetadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Returns the description from collection metadata.
     *
     * @return the description, or {@code null} if metadata is not set
     */
    public String getDescription() {
        return metadata != null ? metadata.getDescription() : null;
    }

    /**
     * Returns the logo URL from collection metadata.
     *
     * @return the logo URL, or {@code null} if metadata is not set
     */
    public String getLogo() {
        return metadata != null ? metadata.getLogoUrl() : null;
    }

    /**
     * Returns the child collection identifiers.
     *
     * @return the child collection ids (empty if metadata is not set)
     */
    public String[] getChildCollections() {
        return metadata != null ? metadata.getChildren() : new String[0];
    }

    /**
     * Returns the parent collection identifiers.
     *
     * @return the parent collection ids (empty if metadata is not set)
     */
    public String[] getParentCollections() {
        return metadata != null ? metadata.getParents() : new String[0];
    }

    /**
     * Returns the annotation location map for this collection.
     *
     * @return the annotation map, or {@code null}
     */
    public Map<String, AorLocation> getAnnotationMap() {
        return annotationMap;
    }

    /**
     * Sets the annotation location map.
     *
     * @param annotationMap the annotation map
     */
    public void setAnnotationMap(Map<String, AorLocation> annotationMap) {
        this.annotationMap = annotationMap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookCollection that)) return false;
        return Objects.equals(id, that.id) &&
                Arrays.equals(books, that.books) &&
                Objects.equals(characterNames, that.characterNames) &&
                Objects.equals(illustrationTitles, that.illustrationTitles) &&
                Objects.equals(narrativeSections, that.narrativeSections) &&
                Objects.equals(checksums, that.checksums) &&
                Objects.equals(missingImage, that.missingImage) &&
                Objects.equals(htmlAnnotations, that.htmlAnnotations) &&
                Objects.equals(peopleRef, that.peopleRef) &&
                Objects.equals(locationsRef, that.locationsRef) &&
                Objects.equals(booksRef, that.booksRef) &&
                Objects.equals(metadata, that.metadata) &&
                Objects.equals(annotationMap, that.annotationMap);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, characterNames, illustrationTitles, narrativeSections,
                checksums, missingImage, htmlAnnotations, peopleRef, locationsRef,
                booksRef, metadata, annotationMap);
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
                ", htmlAnnotations=" + htmlAnnotations +
                ", peopleRef=" + peopleRef +
                ", locationsRef=" + locationsRef +
                ", booksRef=" + booksRef +
                ", metadata=" + metadata +
                ", annotationMap=" + annotationMap +
                '}';
    }
}
