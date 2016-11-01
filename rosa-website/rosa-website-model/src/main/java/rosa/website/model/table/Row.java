package rosa.website.model.table;

import java.io.Serializable;
import java.util.Arrays;

/**
 * A row of string values in a Table. Optionally indexed by an enumeration.
 */
public class Row implements Serializable {
    private static final long serialVersionUID = 1L;

    private String[] values;

    public Row() {}

    /**
     * Create a new row in a CSV.
     *
     * @param values values of each cell
     */
    public Row(String... values) {
        this.values = values;
    }

    public void setValues(String[] values) {
        this.values = values;
    }

    /**
     * @param column column index
     * @return the cell value for this row at column index.
     */
    public String getValue(int column) {
        if (values == null || values.length < column) {
            return null;
        }

        return values[column];
    }

    /**
     * @param e column header
     * @return the cell value for this row at specified column
     */
    public String getValue(Enum<?> col) {
        if (col == null) {
            return null;
        }
        
        return getValue(col.ordinal());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Row)) return false;

        Row row = (Row) o;

        if (!Arrays.equals(values, row.values)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return values != null ? Arrays.hashCode(values) : 0;
    }

    @Override
    public String toString() {
        return Arrays.toString(values);
    }
}
