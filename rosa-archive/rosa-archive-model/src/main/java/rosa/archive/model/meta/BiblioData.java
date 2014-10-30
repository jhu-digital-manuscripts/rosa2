package rosa.archive.model.meta;

import java.io.Serializable;
import java.util.Arrays;

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

    private String language;

    public BiblioData() {}

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
                ", language='" + language + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BiblioData that = (BiblioData) o;

        if (!Arrays.equals(authors, that.authors)) return false;
        if (commonName != null ? !commonName.equals(that.commonName) : that.commonName != null) return false;
        if (currentLocation != null ? !currentLocation.equals(that.currentLocation) : that.currentLocation != null)
            return false;
        if (dateLabel != null ? !dateLabel.equals(that.dateLabel) : that.dateLabel != null) return false;
        if (!Arrays.equals(details, that.details)) return false;
        if (language != null ? !language.equals(that.language) : that.language != null) return false;
        if (material != null ? !material.equals(that.material) : that.material != null) return false;
        if (!Arrays.equals(notes, that.notes)) return false;
        if (origin != null ? !origin.equals(that.origin) : that.origin != null) return false;
        if (repository != null ? !repository.equals(that.repository) : that.repository != null) return false;
        if (shelfmark != null ? !shelfmark.equals(that.shelfmark) : that.shelfmark != null) return false;
        if (title != null ? !title.equals(that.title) : that.title != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (dateLabel != null ? dateLabel.hashCode() : 0);
        result = 31 * result + (currentLocation != null ? currentLocation.hashCode() : 0);
        result = 31 * result + (repository != null ? repository.hashCode() : 0);
        result = 31 * result + (shelfmark != null ? shelfmark.hashCode() : 0);
        result = 31 * result + (origin != null ? origin.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (commonName != null ? commonName.hashCode() : 0);
        result = 31 * result + (material != null ? material.hashCode() : 0);
        result = 31 * result + (details != null ? Arrays.hashCode(details) : 0);
        result = 31 * result + (authors != null ? Arrays.hashCode(authors) : 0);
        result = 31 * result + (notes != null ? Arrays.hashCode(notes) : 0);
        result = 31 * result + (language != null ? language.hashCode() : 0);
        return result;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

}
