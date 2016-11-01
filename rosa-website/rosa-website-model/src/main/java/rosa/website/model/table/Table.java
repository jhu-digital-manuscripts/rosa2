package rosa.website.model.table;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A table of string values. The columns of the table are named by an enum. 
 */
public class Table implements Serializable {
    private static final long serialVersionUID = 1L;

    private Enum<?>[] columns;
    private List<Row> rows;
    
    public Table() {
        this.rows = new ArrayList<>();
    }

    public Table(Enum<?>[] columns, List<Row> rows) {
        this.columns = columns;
        this.rows = rows;
    }

    public Row getRow(int index) {
        return rows.get(index);
    }
    
    public Enum<?>[] columns() {
        return columns;
    }

    /**
     * By default, this checks ID values against the first column of the
     * CSV data. It is recommended that this be overridden to match the
     * requirements of the specific implementing CSV format.
     *
     * @param id id to look for
     * @return the row with associated ID
     */
    public Row getRow(Enum<?> col, String id) {
        for (Row entry : rows) {
            if (entry.getValue(col).equals(id)) {
                return entry;
            }
        }
        return null;
    }

    public String getValue(int row, int col) {
        return rows.get(row).getValue(col);
    }

    public String getValue(int row, Enum<?> col) {
        return rows.get(row).getValue(col);
    }

    public List<Row> rows() {
        return rows;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Table)) return false;

        Table that = (Table) o;

        if (rows != null ? !rows.equals(that.rows) : that.rows != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return rows.hashCode();
    }

    @Override
    public String toString() {
        return rows.toString();
    }
}
