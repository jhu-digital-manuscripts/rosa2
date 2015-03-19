package rosa.website.model.csv;

import java.util.List;

public class NarrativeSectionsCSV extends BaseCSVData<NarrativeSectionsCSV.Column> {
    public enum Column {
        ID("Id"),
        DESCRIPTION("Description"),
        LECOY("Lecoy");

        private String key;

        Column(String key) {
            this.key = key;
        }
    }

    public NarrativeSectionsCSV() {
        super();
    }

    public NarrativeSectionsCSV(String id, List<CSVEntry> rows) {
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
}
