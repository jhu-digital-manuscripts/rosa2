package rosa.website.model.csv;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BookDataCSV implements CSVData<BookDataCSV.Column>, Serializable {

    public enum Column {
        ID ("id"),
        REPO ("repository"),
        SHELFMARK ("shelfmark"),
        COMMON_NAME ("common name"),
        CURRENT_LOCATION ("current location"),
        DATE ("date"),
        ORIGIN ("origin"),
        TYPE ("type"),
        NUM_ILLUS ("number of illustrations"),
        NUM_FOLIOS ("number of folios");

        public final String key;

        Column(String key) {
            this.key = key;
        }
    }

    private static final long serialVersionUID = 1L;

    private String id;
    private List<CSVEntry> rows;

    public BookDataCSV() {
        this.rows = new ArrayList<>();
    }

    /**
     * Create a new BookDataCSV.
     *
     * @param id id
     * @param rows row data
     */
    public BookDataCSV(String id, List<CSVEntry> rows) {
        this.id = id;
        this.rows = rows;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setRows(List<CSVEntry> rows) {
        this.rows.clear();
        this.rows.addAll(rows);
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * @param index row index
     * @return the specified row
     */
    @Override
    public CSVEntry getRow(int index) {
        return rows.get(index);
    }

    /**
     * @param id ID
     * @return entry by ID
     */
    @Override
    public CSVEntry getRow(String id) {
        for (CSVEntry entry : rows) {
            if (entry.getValue(Column.ID).equals(id)) {
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
    @Override
    public String getValue(int row, int col) {
        return rows.get(row).getValue(col);
    }

    /**
     * @param row row index
     * @param column column header
     * @return the cell value of the given row and header
     */
    @Override
    public String getValue(int row, Column column) {
        return rows.get(row).getValue(column);
    }

    @Override
    public int size() {
        return rows.size();
    }

    @Override
    public List<CSVEntry> asList() {
        return new ArrayList<>(rows);
    }

    @Override
    public Column[] columns() {
        return Column.values();
    }

    @Override
    public Iterator<CSVEntry> iterator() {
        return rows.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookDataCSV)) return false;

        BookDataCSV dataCSV = (BookDataCSV) o;

        if (id != null ? !id.equals(dataCSV.id) : dataCSV.id != null) return false;
        if (rows != null ? !rows.equals(dataCSV.rows) : dataCSV.rows != null) return false;

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
        return "BookDataCSV{" +
                "id='" + id + '\'' +
                ", rows=" + rows +
                '}';
    }
}
