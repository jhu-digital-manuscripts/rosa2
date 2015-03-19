package rosa.website.model.csv;

import java.io.Serializable;
import java.util.List;

public class CollectionCSV extends BaseCSVData<CollectionCSV.Column> implements CSVData<CollectionCSV.Column>, Serializable {

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

    public CollectionCSV() {
        super();
    }

    /**
     * Create a new CollectionCSV.
     *
     * @param id id
     * @param rows row data
     */
    public CollectionCSV(String id, List<CSVEntry> rows) {
        super(id, rows);
    }

    @Override
    public Column[] columns() {
        return Column.values();
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

    @Override
    public String toString() {
        return "CollectionCSV{" +
                "id='" + id + '\'' +
                ", rows=" + rows +
                '}';
    }
}
