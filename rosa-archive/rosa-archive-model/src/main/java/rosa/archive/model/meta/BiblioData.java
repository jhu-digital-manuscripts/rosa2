package rosa.archive.model.meta;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

/**
 *
 */
public class BiblioData implements Serializable {
    private static final long serialVersionUID = 1L;

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
    private String[] authors;
    private String[] notes;
    private String[] readers;

    private String language;

    public BiblioData() {
        details = new String[0];
        authors = new String[0];
        notes = new String[0];
        readers = new String[0];
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDateLabel() {
        return dateLabel;
    }

    public void setDateLabel(String dateLabel) {
        this.dateLabel = dateLabel;
    }

    public String getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(String currentLocation) {
        this.currentLocation = currentLocation;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getShelfmark() {
        return shelfmark;
    }

    public void setShelfmark(String shelfmark) {
        this.shelfmark = shelfmark;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String[] getDetails() {
        return details;
    }

    public void setDetails(String[] details) {
        this.details = details;
    }

    public String[] getAuthors() {
        return authors;
    }

    public void setAuthors(String[] authors) {
        this.authors = authors;
    }

    public String[] getNotes() {
        return notes;
    }

    public void setNotes(String[] notes) {
        this.notes = notes;
    }

    public String[] getReaders() {
        return readers;
    }

    public void setReaders(String[] readers) {
        this.readers = readers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BiblioData that = (BiblioData) o;
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
                Objects.equals(language, that.language);
    }

    @Override
    public int hashCode() {

        int result = Objects.hash(title, dateLabel, currentLocation, repository, shelfmark, origin, type, commonName, material, language);
        result = 31 * result + Arrays.hashCode(details);
        result = 31 * result + Arrays.hashCode(authors);
        result = 31 * result + Arrays.hashCode(notes);
        result = 31 * result + Arrays.hashCode(readers);
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
                ", language='" + language + '\'' +
                '}';
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

}
