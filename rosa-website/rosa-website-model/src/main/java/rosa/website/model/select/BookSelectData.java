package rosa.website.model.select;

import java.io.Serializable;

import rosa.website.model.table.BookDataColumn;
import rosa.website.model.table.Row;

/**
 * Similar to a BookDataCSV row. Extended to include more data and tailored to
 * use in book selection.
 */
public class BookSelectData implements Serializable {
    private static final long serialVersionUID = 1L;

    private Row data;
    private boolean hasTranscription;
    private boolean hasImageTagging;
    private boolean hasNarrativeTagging;
    private boolean hasBibliography;

    private DataStatus transcriptionStatus;

    public BookSelectData() {}

    /**
     * Create a new BookSelectionData object backed by BookDataCSV
     *
     * @param data a CSVEntry from BookDataCSV
     * @param hasTranscription .
     * @param hasImageTagging .
     * @param hasNarrativeTagging .
     * @param hasBibliography .
     */
    public BookSelectData(Row data, boolean hasTranscription, boolean hasImageTagging,
                          boolean hasNarrativeTagging, boolean hasBibliography, DataStatus transcriptionStatus) {
        this.data = data;
        this.hasTranscription = hasTranscription;
        this.hasImageTagging = hasImageTagging;
        this.hasNarrativeTagging = hasNarrativeTagging;
        this.hasBibliography = hasBibliography;
        this.transcriptionStatus = transcriptionStatus;
    }

    public void setData(Row data) {
        this.data = data;
    }

    public void setHasTranscription(boolean hasTranscription) {
        this.hasTranscription = hasTranscription;
    }

    public void setHasImageTagging(boolean hasImageTagging) {
        this.hasImageTagging = hasImageTagging;
    }

    public void setHasNarrativeTagging(boolean hasNarrativeTagging) {
        this.hasNarrativeTagging = hasNarrativeTagging;
    }

    public void setHasBibliography(boolean hasBibliography) {
        this.hasBibliography = hasBibliography;
    }

    public DataStatus transcriptionStatus() {
        return transcriptionStatus;
    }

    /** @return Does this book have transcriptions? */
    public boolean hasTranscription() {
        return hasTranscription;
    }

    /** @return Does this book have image (illustration) tagging? */
    public boolean hasImageTagging() {
        return hasImageTagging;
    }

    /** @return Does this book have narrative tagging? */
    public boolean hasNarrativeTagging() {
        return hasNarrativeTagging;
    }

    /** @return Does this book have bibliographic information? */
    public boolean hasBibliography() {
        return hasBibliography;
    }

    /** @return the book's ID */
    public String id() {
        return data == null ? null : data.getValue(BookDataColumn.ID);
    }

    /** @return the book's repository */
    public String repository() {
        return data == null ? null : data.getValue(BookDataColumn.REPO);
    }

    /** @return the book's current shelf mark */
    public String shelfmark() {
        return data == null ? null : data.getValue(BookDataColumn.SHELFMARK);
    }

    /** @return the book's common name */
    public String commonName() {
        return data == null ? null : data.getValue(BookDataColumn.COMMON_NAME);
    }

    /** @return the book's current location */
    public String currentLocation() {
        return data == null ? null : data.getValue(BookDataColumn.CURRENT_LOCATION);
    }

    /** @return the book's publication date (EX: 1561, 16th century, etc) */
    public String date() {
        return data == null ? null : data.getValue(BookDataColumn.DATE);
    }

    /** @return the place of the book's origin (EX: Paris, France) */
    public String origin() {
        return data == null ? null : data.getValue(BookDataColumn.ORIGIN);
    }

    /** @return the book's type (manuscript, printed book, etc) */
    public String type() {
        return data == null ? null : data.getValue(BookDataColumn.TYPE);
    }

    /** @return the number of illustrations in the book */
    public String numberOfIllustrations() {
        return data == null ? null : data.getValue(BookDataColumn.NUM_ILLUS);
    }

    /** @return the number of pages in the book */
    public String numberOfFolios() {
        return data == null ? null : data.getValue(BookDataColumn.NUM_FOLIOS);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookSelectData)) return false;

        BookSelectData that = (BookSelectData) o;

        if (hasTranscription != that.hasTranscription) return false;
        if (hasImageTagging != that.hasImageTagging) return false;
        if (hasNarrativeTagging != that.hasNarrativeTagging) return false;
        if (hasBibliography != that.hasBibliography) return false;
        return !(data != null ? !data.equals(that.data) : that.data != null);

    }

    @Override
    public int hashCode() {
        int result = data != null ? data.hashCode() : 0;
        result = 31 * result + (hasTranscription ? 1 : 0);
        result = 31 * result + (hasImageTagging ? 1 : 0);
        result = 31 * result + (hasNarrativeTagging ? 1 : 0);
        result = 31 * result + (hasBibliography ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "BookSelectData{" +
                "data=" + data +
                ", hasTranscription=" + hasTranscription +
                ", hasImageTagging=" + hasImageTagging +
                ", hasNarrativeTagging=" + hasNarrativeTagging +
                ", hasBibliography=" + hasBibliography +
                '}';
    }
}
