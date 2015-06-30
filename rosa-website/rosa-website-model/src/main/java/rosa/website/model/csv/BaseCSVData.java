package rosa.website.model.csv;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class BaseCSVData <T extends Enum> implements CSVData<T>, Serializable {
    private static final long serialVersionUID = 1L;

    protected String id;
    protected List<CSVRow> rows;

    /** Empty constructor for GWT compatibility */
    BaseCSVData() {
        this.rows = new ArrayList<>();
    }

    BaseCSVData(String id, List<CSVRow> rows) {
        this.id = id;
        this.rows = rows;
    }

    protected void setId(String id) {
        this.id = id;
    }

    protected void setRows(List<CSVRow> rows) {
        this.rows = rows;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public CSVRow getRow(int index) {
        return rows.get(index);
    }

    /**
     * By default, this checks ID values against the first column of the
     * CSV data. It is recommended that this be overridden to match the
     * requirements of the specific implementing CSV format.
     *
     * @param id id to look for
     * @return the row with associated ID
     */
    @Override
    public CSVRow getRow(String id) {
        for (CSVRow entry : rows) {
            if (entry.getValue(0).equals(id)) {
                return entry;
            }
        }
        return null;
    }

    @Override
    public String getValue(int row, int col) {
        return rows.get(row).getValue(col);
    }

    @Override
    public String getValue(int row, T col) {
        return rows.get(row).getValue(col);
    }

    @Override
    public List<CSVRow> asList() {
        return new ArrayList<>(rows);
    }

    @Override
    public Iterator<CSVRow> iterator() {
        return rows.iterator();
    }

    @Override
    public int size() {
        return rows.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseCSVData)) return false;

        BaseCSVData that = (BaseCSVData) o;

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
        return "BaseCSVData{" +
                "id='" + id + '\'' +
                ", rows=" + rows +
                '}';
    }
}
