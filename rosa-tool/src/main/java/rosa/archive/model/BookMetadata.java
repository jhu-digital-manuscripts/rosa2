package rosa.archive.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Metadata describing a book in the archive.
 * Bibliographic data is keyed by language code, allowing access to
 * localized descriptions of the same book.
 */
public class BookMetadata implements HasId {

    private String id;
    private int yearStart;
    private int yearEnd;
    private String dimensionUnits;
    private int width;
    private int height;
    private int numberOfIllustrations;
    private int numberOfPages;
    private String licenseUrl;
    private String licenseLogo;
    private List<BookText> bookTexts;
    private Map<String, BiblioData> biblioDataMap;

    /**
     * Creates an empty BookMetadata with default values (-1 for numeric fields).
     */
    public BookMetadata() {
        yearStart = -1;
        yearEnd = -1;
        width = -1;
        height = -1;
        numberOfIllustrations = -1;
        numberOfPages = -1;
        bookTexts = new ArrayList<>();
        biblioDataMap = new HashMap<>();
    }

    /**
     * Returns a formatted dimensions string (e.g., "200mm x 300mm").
     *
     * @return the dimensions string
     */
    public String getDimensionsString() {
        return width + dimensionUnits + " x " + height + dimensionUnits;
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
     * Returns the earliest year associated with this book.
     *
     * @return the start year, or -1 if unknown
     */
    public int getYearStart() {
        return yearStart;
    }

    /**
     * Sets the earliest year associated with this book.
     *
     * @param yearStart the start year
     */
    public void setYearStart(int yearStart) {
        this.yearStart = yearStart;
    }

    /**
     * Returns the latest year associated with this book.
     *
     * @return the end year, or -1 if unknown
     */
    public int getYearEnd() {
        return yearEnd;
    }

    /**
     * Sets the latest year associated with this book.
     *
     * @param yearEnd the end year
     */
    public void setYearEnd(int yearEnd) {
        this.yearEnd = yearEnd;
    }

    /**
     * Returns the unit of measure for dimensions (e.g., "mm", "cm").
     *
     * @return the dimension units, or {@code null} if not set
     */
    public String getDimensionUnits() {
        return dimensionUnits;
    }

    /**
     * Sets the unit of measure for dimensions.
     *
     * @param dimensionUnits the dimension units
     */
    public void setDimensionUnits(String dimensionUnits) {
        this.dimensionUnits = dimensionUnits;
    }

    /**
     * Returns the book width in the specified dimension units.
     *
     * @return the width, or -1 if unknown
     */
    public int getWidth() {
        return width;
    }

    /**
     * Sets the book width.
     *
     * @param width the width
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * Returns the book height in the specified dimension units.
     *
     * @return the height, or -1 if unknown
     */
    public int getHeight() {
        return height;
    }

    /**
     * Sets the book height.
     *
     * @param height the height
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Returns the number of illustrations in this book.
     *
     * @return the number of illustrations, or -1 if unknown
     */
    public int getNumberOfIllustrations() {
        return numberOfIllustrations;
    }

    /**
     * Sets the number of illustrations.
     *
     * @param numberOfIllustrations the number of illustrations
     */
    public void setNumberOfIllustrations(int numberOfIllustrations) {
        this.numberOfIllustrations = numberOfIllustrations;
    }

    /**
     * Returns the number of pages in this book.
     *
     * @return the number of pages, or -1 if unknown
     */
    public int getNumberOfPages() {
        return numberOfPages;
    }

    /**
     * Sets the number of pages.
     *
     * @param numberOfPages the number of pages
     */
    public void setNumberOfPages(int numberOfPages) {
        this.numberOfPages = numberOfPages;
    }

    /**
     * Returns the map of language code to bibliographic data.
     *
     * @return the biblio data map
     */
    public Map<String, BiblioData> getBiblioDataMap() {
        return biblioDataMap;
    }

    /**
     * Sets the map of language code to bibliographic data.
     *
     * @param biblioDataMap the biblio data map
     */
    public void setBiblioDataMap(Map<String, BiblioData> biblioDataMap) {
        this.biblioDataMap = biblioDataMap;
    }

    /**
     * Returns the list of texts contained in this book.
     *
     * @return the book texts
     */
    public List<BookText> getBookTexts() {
        return bookTexts;
    }

    /**
     * Sets the list of texts contained in this book.
     *
     * @param bookTexts the book texts
     */
    public void setBookTexts(List<BookText> bookTexts) {
        this.bookTexts = bookTexts;
    }

    /**
     * Checks whether bibliographic data is available in the specified language.
     *
     * @param languageCode the language code to check
     * @return {@code true} if data exists for the language
     */
    public boolean supportsLanguage(String languageCode) {
        return biblioDataMap.containsKey(languageCode);
    }

    /**
     * Returns the license URL for this book.
     *
     * @return the license URL, or {@code null} if not set
     */
    public String getLicenseUrl() {
        return licenseUrl;
    }

    /**
     * Sets the license URL.
     *
     * @param licenseUrl the license URL
     */
    public void setLicenseUrl(String licenseUrl) {
        this.licenseUrl = licenseUrl;
    }

    /**
     * Returns the license logo URL.
     *
     * @return the license logo URL, or {@code null} if not set
     */
    public String getLicenseLogo() {
        return licenseLogo;
    }

    /**
     * Sets the license logo URL.
     *
     * @param licenseLogo the license logo URL
     */
    public void setLicenseLogo(String licenseLogo) {
        this.licenseLogo = licenseLogo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookMetadata that)) return false;
        return yearStart == that.yearStart &&
                yearEnd == that.yearEnd &&
                width == that.width &&
                height == that.height &&
                numberOfIllustrations == that.numberOfIllustrations &&
                numberOfPages == that.numberOfPages &&
                Objects.equals(id, that.id) &&
                Objects.equals(dimensionUnits, that.dimensionUnits) &&
                Objects.equals(licenseUrl, that.licenseUrl) &&
                Objects.equals(licenseLogo, that.licenseLogo) &&
                Objects.equals(bookTexts, that.bookTexts) &&
                Objects.equals(biblioDataMap, that.biblioDataMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, yearStart, yearEnd, dimensionUnits, width, height,
                numberOfIllustrations, numberOfPages, licenseUrl, licenseLogo,
                bookTexts, biblioDataMap);
    }

    @Override
    public String toString() {
        return "BookMetadata{" +
                "id='" + id + '\'' +
                ", yearStart=" + yearStart +
                ", yearEnd=" + yearEnd +
                ", dimensionUnits='" + dimensionUnits + '\'' +
                ", width=" + width +
                ", height=" + height +
                ", numberOfIllustrations=" + numberOfIllustrations +
                ", numberOfPages=" + numberOfPages +
                ", licenseUrl='" + licenseUrl + '\'' +
                ", licenseLogo='" + licenseLogo + '\'' +
                ", bookTexts=" + bookTexts +
                ", biblioDataMap=" + biblioDataMap +
                '}';
    }
}
