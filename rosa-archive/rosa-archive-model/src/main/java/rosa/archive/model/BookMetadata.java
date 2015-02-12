package rosa.archive.model;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Metadata describing a book in the archive.
 */
public final class BookMetadata implements HasId, Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String title;
    private String date;
    private int yearStart;
    private int yearEnd;
    private String currentLocation;
    private String repository;
    private String shelfmark;
    private String origin;
    private String dimensions;
    private String dimensionUnits;
    private int width;
    private int height;
    private int numberOfIllustrations;
    private int numberOfPages;
    private String type;
    private String commonName;
    private String material;
    private BookText[] texts;

    /**
     * Create empty BookMetadata.
     */
    public BookMetadata() {
        yearStart = -1;
        yearEnd = -1;
        width = -1;
        height = -1;
        numberOfIllustrations = -1;
        numberOfPages = -1;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

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

    public String getDimensionUnits() {
        return dimensionUnits;
    }

    public void setDimensionUnits(String dimensionUnits) {
        this.dimensionUnits = dimensionUnits;
    }

    public BookText[] getTexts() {
        return texts;
    }

    public void setTexts(BookText[] texts) {
        this.texts = texts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BookMetadata metadata = (BookMetadata) o;

        if (height != metadata.height) return false;
        if (numberOfIllustrations != metadata.numberOfIllustrations) return false;
        if (numberOfPages != metadata.numberOfPages) return false;
        if (width != metadata.width) return false;
        if (yearEnd != metadata.yearEnd) return false;
        if (yearStart != metadata.yearStart) return false;
        if (commonName != null ? !commonName.equals(metadata.commonName) : metadata.commonName != null) return false;
        if (currentLocation != null ? !currentLocation.equals(metadata.currentLocation) : metadata.currentLocation != null)
            return false;
        if (date != null ? !date.equals(metadata.date) : metadata.date != null) return false;
        if (dimensionUnits != null ? !dimensionUnits.equals(metadata.dimensionUnits) : metadata.dimensionUnits != null)
            return false;
        if (dimensions != null ? !dimensions.equals(metadata.dimensions) : metadata.dimensions != null) return false;
        if (id != null ? !id.equals(metadata.id) : metadata.id != null) return false;
        if (material != null ? !material.equals(metadata.material) : metadata.material != null) return false;
        if (origin != null ? !origin.equals(metadata.origin) : metadata.origin != null) return false;
        if (repository != null ? !repository.equals(metadata.repository) : metadata.repository != null) return false;
        if (shelfmark != null ? !shelfmark.equals(metadata.shelfmark) : metadata.shelfmark != null) return false;
        if (!Arrays.equals(texts, metadata.texts)) return false;
        if (title != null ? !title.equals(metadata.title) : metadata.title != null) return false;
        if (type != null ? !type.equals(metadata.type) : metadata.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (date != null ? date.hashCode() : 0);
        result = 31 * result + yearStart;
        result = 31 * result + yearEnd;
        result = 31 * result + (currentLocation != null ? currentLocation.hashCode() : 0);
        result = 31 * result + (repository != null ? repository.hashCode() : 0);
        result = 31 * result + (shelfmark != null ? shelfmark.hashCode() : 0);
        result = 31 * result + (origin != null ? origin.hashCode() : 0);
        result = 31 * result + (dimensions != null ? dimensions.hashCode() : 0);
        result = 31 * result + (dimensionUnits != null ? dimensionUnits.hashCode() : 0);
        result = 31 * result + width;
        result = 31 * result + height;
        result = 31 * result + numberOfIllustrations;
        result = 31 * result + numberOfPages;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (commonName != null ? commonName.hashCode() : 0);
        result = 31 * result + (material != null ? material.hashCode() : 0);
        result = 31 * result + (texts != null ? Arrays.hashCode(texts) : 0);
        return result;
    }

    @Override
    public String toString() {
        return "BookMetadata{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", date='" + date + '\'' +
                ", yearStart=" + yearStart +
                ", yearEnd=" + yearEnd +
                ", currentLocation='" + currentLocation + '\'' +
                ", repository='" + repository + '\'' +
                ", shelfmark='" + shelfmark + '\'' +
                ", origin='" + origin + '\'' +
                ", dimensions='" + dimensions + '\'' +
                ", dimensionUnits='" + dimensionUnits + '\'' +
                ", width=" + width +
                ", height=" + height +
                ", numberOfIllustrations=" + numberOfIllustrations +
                ", numberOfPages=" + numberOfPages +
                ", type='" + type + '\'' +
                ", commonName='" + commonName + '\'' +
                ", material='" + material + '\'' +
                ", texts=" + Arrays.toString(texts) +
                '}';
    }
}
