package rosa.website.model.csv;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;

public class CSVEntry implements Iterable<String>, Serializable {
    private static final long serialVersionUID = 1L;

    private final String[] values;

    /**
     * Create a new row in a CSV.
     *
     * @param values values of each cell
     */
    public CSVEntry(String ... values) {
        this.values = values;
    }

    /**
     * @param column column index
     * @return the cell value for this row at column index.
     */
    public String getValue(int column) {
        return values[column];
    }

    /**
     * @param e column header
     * @return the cell value for this row at specified column
     */
    public String getValue(Enum<?> e) {
        return values[e.ordinal()];
    }

    @Override
    public Iterator<String> iterator() {
        return Arrays.asList(values).iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CSVEntry)) return false;

        CSVEntry strings = (CSVEntry) o;

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
