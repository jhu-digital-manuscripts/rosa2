package rosa.website.model.csv;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

public class CollectionCSV implements CSVData<CollectionCSV.Column>, Serializable {

    // collection_data.csv
    public enum Column {
        ID ("id"),
        NAME ("name"),
        ORIGIN ("origin"),
        MATERIAL ("material"),
        NUM_FOLIOS ("number of folios"),
        HEIGHT ("height (mm)"),
        WIDTH ("width (mm)"),
        LEAVES_PER_GATHERING ("leaves per gathering"),
        LINES_PER_COLUMN ("lines per column"),
        NUM_ILLUS ("number of illustrations"),
        DATE_START ("date start"),
        DATE_END ("date end"),
        COLUMNS_PER_FOLIO ("columns per folio"),
        TEXTS ("texts"),
        FOLIOS_ONE_ILLUS ("folios with one illustration"),
        FOLIOS_MORE_ILLUS ("folios with more than one illustration");

        public final String key;

        Column(String key) {
            this.key = key;
        }
    }

    private static final long serialVersionUID = 1L;

    private final String id;
    private final List<CSVEntry> rows;

    /**
     * Create a new CollectionCSV.
     *
     * @param id id
     * @param rows row data
     */
    public CollectionCSV(String id, List<CSVEntry> rows) {
        this.id = id;
        this.rows = rows;
    }

    @Override
    public Column[] columns() {
        return Column.values();
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
        return getRow(row).getValue(col);
    }

    /**
     * @param row row index
     * @param header column header
     * @return the cell value of the given row and header
     */
    @Override
    public String getValue(int row, Column header) {
        return getRow(row).getValue(header);
    }

    @Override
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
        if (!(o instanceof CollectionCSV)) return false;

        CollectionCSV that = (CollectionCSV) o;

        if (rows != null ? !rows.equals(that.rows) : that.rows != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return rows != null ? rows.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "CollectionCSV{" +
                "rows=" + rows +
                '}';
    }
}
