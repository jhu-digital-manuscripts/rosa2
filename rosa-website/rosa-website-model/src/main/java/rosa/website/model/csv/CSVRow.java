package rosa.website.model.csv;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;

public class CSVRow implements Iterable<String>, Serializable {
    private static final long serialVersionUID = 1L;

    private String[] values;

    public CSVRow() {}

    /**
     * Create a new row in a CSV.
     *
     * @param values values of each cell
     */
    public CSVRow(String... values) {
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
    public String getValue(Enum<?> e) {
        if (e == null) {
            return null;
        }
        return getValue(e.ordinal());
    }

    @Override
    public Iterator<String> iterator() {
        return Arrays.asList(values).iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CSVRow)) return false;

        CSVRow strings = (CSVRow) o;

        if (!Arrays.equals(values, strings.values)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return values != null ? Arrays.hashCode(values) : 0;
    }

    @Override
    public String toString() {
        return "CSVEntry{" +
                "values=" + Arrays.toString(values) +
                '}';
    }
}
