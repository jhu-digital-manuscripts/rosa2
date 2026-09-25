package rosa.archive.model;

import java.util.Arrays;
import java.util.Objects;

/**
 * Bibliographic data for a book in a specific language.
 * Contains descriptive metadata fields such as title, repository, shelfmark,
 * and references to authors and readers.
 */
public class BiblioData {

    private String title;
    private String dateLabel;
    private String currentLocation;
    private String repository;
    private String shelfmark;
    private String origin;
    private String type;
    private String commonName;
    private String material;
    private String[] details;
    private ObjectRef[] authors;
    private String[] notes;
    private ObjectRef[] readers;
    private String[] websites;
    private String language;

    /**
     * Creates an empty BiblioData with empty arrays for collection fields.
     */
    public BiblioData() {
        details = new String[0];
        authors = new ObjectRef[0];
        notes = new String[0];
        readers = new ObjectRef[0];
        websites = new String[0];
    }

    /**
     * Returns the title of the book.
     *
     * @return the title, or {@code null} if not set
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title of the book.
     *
     * @param title the title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the date label (human-readable date description).
     *
     * @return the date label, or {@code null} if not set
     */
    public String getDateLabel() {
        return dateLabel;
    }

    /**
     * Sets the date label.
     *
     * @param dateLabel the date label
     */
    public void setDateLabel(String dateLabel) {
        this.dateLabel = dateLabel;
    }

    /**
     * Returns the current location of the physical book.
     *
     * @return the current location, or {@code null} if not set
     */
    public String getCurrentLocation() {
        return currentLocation;
    }

    /**
     * Sets the current location of the physical book.
     *
     * @param currentLocation the current location
     */
    public void setCurrentLocation(String currentLocation) {
        this.currentLocation = currentLocation;
    }

    /**
     * Returns the repository holding the book.
     *
     * @return the repository, or {@code null} if not set
     */
    public String getRepository() {
        return repository;
    }

    /**
     * Sets the repository holding the book.
     *
     * @param repository the repository
     */
    public void setRepository(String repository) {
        this.repository = repository;
    }

    /**
     * Returns the shelfmark (library catalog identifier).
     *
     * @return the shelfmark, or {@code null} if not set
     */
    public String getShelfmark() {
        return shelfmark;
    }

    /**
     * Sets the shelfmark.
     *
     * @param shelfmark the shelfmark
     */
    public void setShelfmark(String shelfmark) {
        this.shelfmark = shelfmark;
    }

    /**
     * Returns the origin (place of production) of the book.
     *
     * @return the origin, or {@code null} if not set
     */
    public String getOrigin() {
        return origin;
    }

    /**
     * Sets the origin.
     *
     * @param origin the origin
     */
    public void setOrigin(String origin) {
        this.origin = origin;
    }

    /**
     * Returns the type of the book (e.g., manuscript, printed).
     *
     * @return the type, or {@code null} if not set
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type of the book.
     *
     * @param type the type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns the common name used to refer to this book.
     *
     * @return the common name, or {@code null} if not set
     */
    public String getCommonName() {
        return commonName;
    }

    /**
     * Sets the common name.
     *
     * @param commonName the common name
     */
    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    /**
     * Returns the material the book is made of (e.g., parchment, paper).
     *
     * @return the material, or {@code null} if not set
     */
    public String getMaterial() {
        return material;
    }

    /**
     * Sets the material.
     *
     * @param material the material
     */
    public void setMaterial(String material) {
        this.material = material;
    }

    /**
     * Returns additional details about the book.
     *
     * @return the details array
     */
    public String[] getDetails() {
        return details;
    }

    /**
     * Sets additional details.
     *
     * @param details the details array
     */
    public void setDetails(String[] details) {
        this.details = details;
    }

    /**
     * Returns the authors of the book.
     *
     * @return the authors array
     */
    public ObjectRef[] getAuthors() {
        return authors;
    }

    /**
     * Sets the authors.
     *
     * @param authors the authors array
     */
    public void setAuthors(ObjectRef[] authors) {
        this.authors = authors;
    }

    /**
     * Returns notes about the book.
     *
     * @return the notes array
     */
    public String[] getNotes() {
        return notes;
    }

    /**
     * Sets notes about the book.
     *
     * @param notes the notes array
     */
    public void setNotes(String[] notes) {
        this.notes = notes;
    }

    /**
     * Returns the known readers of the book.
     *
     * @return the readers array
     */
    public ObjectRef[] getReaders() {
        return readers;
    }

    /**
     * Sets the known readers.
     *
     * @param readers the readers array
     */
    public void setReaders(ObjectRef[] readers) {
        this.readers = readers;
    }

    /**
     * Returns websites related to this book.
     *
     * @return the websites array
     */
    public String[] getWebsites() {
        return websites;
    }

    /**
     * Sets websites related to this book.
     *
     * @param websites the websites array
     */
    public void setWebsites(String[] websites) {
        this.websites = websites;
    }

    /**
     * Returns the language code for this bibliographic data entry.
     *
     * @return the language code, or {@code null} if not set
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Sets the language code for this bibliographic data entry.
     *
     * @param language the language code
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BiblioData that)) return false;
        return Objects.equals(title, that.title) &&
                Objects.equals(dateLabel, that.dateLabel) &&
                Objects.equals(currentLocation, that.currentLocation) &&
                Objects.equals(repository, that.repository) &&
                Objects.equals(shelfmark, that.shelfmark) &&
                Objects.equals(origin, that.origin) &&
                Objects.equals(type, that.type) &&
                Objects.equals(commonName, that.commonName) &&
                Objects.equals(material, that.material) &&
                Arrays.equals(details, that.details) &&
                Arrays.equals(authors, that.authors) &&
                Arrays.equals(notes, that.notes) &&
                Arrays.equals(readers, that.readers) &&
                Arrays.equals(websites, that.websites) &&
                Objects.equals(language, that.language);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(title, dateLabel, currentLocation, repository,
                shelfmark, origin, type, commonName, material, language);
        result = 31 * result + Arrays.hashCode(details);
        result = 31 * result + Arrays.hashCode(authors);
        result = 31 * result + Arrays.hashCode(notes);
        result = 31 * result + Arrays.hashCode(readers);
        result = 31 * result + Arrays.hashCode(websites);
        return result;
    }

    @Override
    public String toString() {
        return "BiblioData{" +
                "title='" + title + '\'' +
                ", dateLabel='" + dateLabel + '\'' +
                ", currentLocation='" + currentLocation + '\'' +
                ", repository='" + repository + '\'' +
                ", shelfmark='" + shelfmark + '\'' +
                ", origin='" + origin + '\'' +
                ", type='" + type + '\'' +
                ", commonName='" + commonName + '\'' +
                ", material='" + material + '\'' +
                ", details=" + Arrays.toString(details) +
                ", authors=" + Arrays.toString(authors) +
                ", notes=" + Arrays.toString(notes) +
                ", readers=" + Arrays.toString(readers) +
                ", websites=" + Arrays.toString(websites) +
                ", language='" + language + '\'' +
                '}';
    }
}
