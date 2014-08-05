package rosa.archive.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Collection;

/**
 * Metadata describing a book in the archive.
 */
public class BookMetadata implements IsSerializable {

    private String date;
    private int yearStart;
    private int yearEnd;
    private String currentLocation;
    private String repository;
    private String shelfmark;
    private String origin;
    private String dimensions;
    private int width;
    private int height;
    private int numberOfIllustrations;
    private int numberOfPages;
    private String type;
    private String commonName;
    private String material;
    private Collection<BookText> texts;

    public BookMetadata() {  }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getYearStart() {
        return yearStart;
    }

    public void setYearStart(int yearStart) {
        this.yearStart = yearStart;
    }

    public int getYearEnd() {
        return yearEnd;
    }

    public void setYearEnd(int yearEnd) {
        this.yearEnd = yearEnd;
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

    public String getDimensions() {
        return dimensions;
    }

    public void setDimensions(String dimensions) {
        this.dimensions = dimensions;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getNumberOfIllustrations() {
        return numberOfIllustrations;
    }

    public void setNumberOfIllustrations(int numberOfIllustrations) {
        this.numberOfIllustrations = numberOfIllustrations;
    }

    public int getNumberOfPages() {
        return numberOfPages;
    }

    public void setNumberOfPages(int numberOfPages) {
        this.numberOfPages = numberOfPages;
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

    public Collection<BookText> getTexts() {
        return texts;
    }

    public void setTexts(Collection<BookText> texts) {
        this.texts = texts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookMetadata)) return false;

        BookMetadata that = (BookMetadata) o;

        if (height != that.height) return false;
        if (numberOfIllustrations != that.numberOfIllustrations) return false;
        if (numberOfPages != that.numberOfPages) return false;
        if (width != that.width) return false;
        if (yearEnd != that.yearEnd) return false;
        if (yearStart != that.yearStart) return false;
        if (commonName != null ? !commonName.equals(that.commonName) : that.commonName != null) return false;
        if (currentLocation != null ? !currentLocation.equals(that.currentLocation) : that.currentLocation != null)
            return false;
        if (date != null ? !date.equals(that.date) : that.date != null) return false;
        if (dimensions != null ? !dimensions.equals(that.dimensions) : that.dimensions != null) return false;
        if (material != null ? !material.equals(that.material) : that.material != null) return false;
        if (origin != null ? !origin.equals(that.origin) : that.origin != null) return false;
        if (repository != null ? !repository.equals(that.repository) : that.repository != null) return false;
        if (shelfmark != null ? !shelfmark.equals(that.shelfmark) : that.shelfmark != null) return false;
        if (texts != null ? !texts.equals(that.texts) : that.texts != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = date != null ? date.hashCode() : 0;
        result = 31 * result + yearStart;
        result = 31 * result + yearEnd;
        result = 31 * result + (currentLocation != null ? currentLocation.hashCode() : 0);
        result = 31 * result + (repository != null ? repository.hashCode() : 0);
        result = 31 * result + (shelfmark != null ? shelfmark.hashCode() : 0);
        result = 31 * result + (origin != null ? origin.hashCode() : 0);
        result = 31 * result + (dimensions != null ? dimensions.hashCode() : 0);
        result = 31 * result + width;
        result = 31 * result + height;
        result = 31 * result + numberOfIllustrations;
        result = 31 * result + numberOfPages;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (commonName != null ? commonName.hashCode() : 0);
        result = 31 * result + (material != null ? material.hashCode() : 0);
        result = 31 * result + (texts != null ? texts.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "BookMetadata{" +
                "date='" + date + '\'' +
                ", yearStart=" + yearStart +
                ", yearEnd=" + yearEnd +
                ", currentLocation='" + currentLocation + '\'' +
                ", repository='" + repository + '\'' +
                ", shelfmark='" + shelfmark + '\'' +
                ", origin='" + origin + '\'' +
                ", dimensions='" + dimensions + '\'' +
                ", width=" + width +
                ", height=" + height +
                ", numberOfIllustrations=" + numberOfIllustrations +
                ", numberOfPages=" + numberOfPages +
                ", type='" + type + '\'' +
                ", commonName='" + commonName + '\'' +
                ", material='" + material + '\'' +
                ", texts=" + texts +
                '}';
    }
}
