package rosa.website.model.csv;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

public class IllustrationTitleCSV implements Iterable<CSVEntry>, Serializable {

    public enum Column {
        LOCATION ("location"),
        TITLE ("title"),
        FREQUENCY ("frequency");

        public final String key;

        Column(String key) {
            this.key = key;
        }
    }

    private static final long serialVersionUID = 1L;

    private final String id;
    private final List<CSVEntry> rows;

    /**
     * Create a new IllustrationTitleCSV.
     *
     * @param id id
     * @param rows row data
     */
    public IllustrationTitleCSV(String id, List<CSVEntry> rows) {
        this.id = id;
        this.rows = rows;
    }

    public String getId() {
        return id;
    }

    /**
     * @param index row index
     * @return the specified row
     */
    public CSVEntry getRow(int index) {
        return rows.get(index);
    }

    /**
     * @param title title
     * @return entry by title
     */
    public CSVEntry getRow(String title) {
        for (CSVEntry entry : rows) {
            if (entry.getValue(Column.TITLE).equals(title)) {
                return entry;
            }
        }

        return null;
    }

    /**
     * @param row row index
     * @param col column index
     * @return the cell value of the given row, column
     */
    public String getValue(int row, int col) {
        return rows.get(row).getValue(col);
    }

    /**
     * @param row row index
     * @param column column header
     * @return the cell value of the given row and header
     */
    public String getValue(int row, Column column) {
        return rows.get(row).getValue(column);
    }

    public int size() {
        return rows.size();
    }

    @Override
    public Iterator<CSVEntry> iterator() {
        return rows.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IllustrationTitleCSV)) return false;

        IllustrationTitleCSV that = (IllustrationTitleCSV) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (rows != null ? !rows.equals(that.rows) : that.rows != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (rows != null ? rows.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "IllustrationTitleCSV{" +
                "id='" + id + '\'' +
                ", rows=" + rows +
                '}';
    }
}
