package rosa.website.model.csv;

import java.io.Serializable;
import java.util.List;

public class IllustrationTitleCSV extends BaseCSVData<IllustrationTitleCSV.Column> implements CSVData<IllustrationTitleCSV.Column>, Serializable {

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

    public IllustrationTitleCSV() {
        super();
    }

    /**
     * Create a new IllustrationTitleCSV.
     *
     * @param id id
     * @param rows row data
     */
    public IllustrationTitleCSV(String id, List<CSVEntry> rows) {
        super(id, rows);
    }

    @Override
    public Column[] columns() {
        return Column.values();
    }

    /**
     * @param title title
     * @return entry by title
     */
    @Override
    public CSVEntry getRow(String title) {
        for (CSVEntry entry : rows) {
            if (entry.getValue(Column.TITLE).equals(title)) {
                return entry;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return "IllustrationTitleCSV{" +
                "id='" + id + '\'' +
                ", rows=" + rows +
                '}';
    }
}
