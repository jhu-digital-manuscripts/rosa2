package rosa.website.model.csv;

import java.io.Serializable;
import java.util.List;

public class BookDataCSV extends BaseCSVData<BookDataCSV.Column> implements CSVData<BookDataCSV.Column>, Serializable {

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

    public BookDataCSV() {
        super();
    }

    /**
     * Create a new BookDataCSV.
     *
     * @param id id
     * @param rows row data
     */
    public BookDataCSV(String id, List<CSVRow> rows) {
        super(id, rows);
    }

    /**
     * @param id ID
     * @return entry by ID
     */
    @Override
    public CSVRow getRow(String id) {
        for (CSVRow entry : rows) {
            if (entry.getValue(Column.ID).equals(id)) {
                return entry;
            }
        }

        return null;
    }

    @Override
    public Column[] columns() {
        return Column.values();
    }

    @Override
    public String toString() {
        return "BookDataCSV{" +
                "id='" + id + '\'' +
                ", rows=" + rows +
                '}';
    }
}
