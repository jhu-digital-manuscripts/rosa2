package rosa.website.model.csv;

import rosa.website.model.csv.CollectionCSV.Column;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CollectionDisplayCSV extends BaseCSVData<CollectionDisplayCSV.Column> implements CSVData<CollectionDisplayCSV.Column>, Serializable {

    public enum Column {
        NAME("name"),
        DATE("date"),
        FOLIOS("number of folios"),
        ILLUSTRATIONS("number of illustrations"),
        COLUMNS("number of columns/folio"),
        LINES("number of lines/column"),
        SIZE("size (mm)"),
        STRUCT("structure"),
        MORE_THAN_ONE_ILLUSTRATION("number of folios with more than one illustration");

        public final String key;

        Column(String key) {
            this.key = key;
        }
    }

    private static final long serialVersionUID = 1L;

    /** No-arg constructor for GWT serialization */
    CollectionDisplayCSV() {}

    public CollectionDisplayCSV(String id, List<CSVRow> rows) {
        super(id, rows);
    }

    public CollectionDisplayCSV(CollectionCSV collectionCSV) {
        this(collectionCSV.getId(), null);
        setRows(adaptCollectionCSV(collectionCSV));
    }

    private List<CSVRow> adaptCollectionCSV(CollectionCSV collectionCSV) {
        List<CSVRow> rows = new ArrayList<>();

        for (CSVRow row : collectionCSV) {
            String date = row.getValue(CollectionCSV.Column.DATE_START) + "-"
                    + row.getValue(CollectionCSV.Column.DATE_END);
            String size = row.getValue(CollectionCSV.Column.WIDTH) + "x"
                    + row.getValue(CollectionCSV.Column.HEIGHT);

            rows.add(new CSVRow(
                    row.getValue(CollectionCSV.Column.ID),
                    date.contains("null") || date.contains("NULL") ? "" : date,
                    row.getValue(CollectionCSV.Column.NUM_FOLIOS),
                    row.getValue(CollectionCSV.Column.NUM_ILLUS),
                    row.getValue(CollectionCSV.Column.COLUMNS_PER_FOLIO),
                    row.getValue(CollectionCSV.Column.LINES_PER_COLUMN),
                    size.contains("null") || size.contains("NULL") ? "" : size,
                    "",
                    row.getValue(CollectionCSV.Column.FOLIOS_MORE_ILLUS)
            ));
        }

        return rows;
    }

    @Override
    public Column[] columns() {
        return Column.values();
    }



}
