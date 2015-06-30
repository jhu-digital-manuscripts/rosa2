package rosa.website.model.csv;

import java.io.Serializable;
import java.util.List;

public class CharacterNamesCSV extends BaseCSVData<CharacterNamesCSV.Column> implements Serializable {

    public enum Column {
        NAME("name"),
        FRENCH("french"),
        ENGLISH("english");

        private String key;

        Column(String key) {
            this.key = key;
        }
    }

    private static final long serialVersionUID = 1L;

    public CharacterNamesCSV() {
        super();
    }

    public CharacterNamesCSV(String id, List<CSVRow> rows) {
        super(id, rows);
    }

    @Override
    public Column[] columns() {
        return Column.values();
    }

    @Override
    public CSVRow getRow(String id) {
        for (CSVRow entry : rows) {
            if (entry.getValue(Column.NAME).equals(id)) {
                return entry;
            }
        }

        return null;
    }
}
