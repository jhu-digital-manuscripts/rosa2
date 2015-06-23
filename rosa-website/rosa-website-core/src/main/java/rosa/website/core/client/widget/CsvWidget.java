package rosa.website.core.client.widget;

import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.view.client.ListDataProvider;
import rosa.website.model.csv.CSVData;
import rosa.website.model.csv.CSVEntry;

import java.util.Comparator;
import java.util.logging.Logger;

public class CsvWidget extends Composite {
    private static final Logger logger = Logger.getLogger(CsvWidget.class.toString());
    private static final String NUM_REGEX = "^-?\\d+(\\.\\d+)?$";

    private final CellTable<CSVEntry> table;
    private final ListDataProvider<CSVEntry> dataProvider;

    /**
     * Create a new blank CsvWidget.
     */
    public CsvWidget() {
        SimplePanel root = new SimplePanel();

        this.table = new CellTable<>();
        this.dataProvider = new ListDataProvider<>();

        dataProvider.addDataDisplay(table);

        root.setSize("100%", "100%");
        root.setWidget(table);

        initWidget(root);
    }

    @SuppressWarnings("unchecked")
    public void setData(CSVData data) {
        clear();

        dataProvider.setList(data.asList());
        ListHandler<CSVEntry> sortHandler = new ListHandler<CSVEntry>(dataProvider.getList()) {
            @Override
            public void onColumnSort(ColumnSortEvent event) {
                super.onColumnSort(event);
                dataProvider.refresh();
            }
        };
        createColumns(data, sortHandler);

        table.addColumnSortHandler(sortHandler);
        table.flush();
    }

    /**
     * Clear the data table of all data.
     */
    public void clear() {
        for (int i = table.getColumnCount() - 1; i >= 0; i--) {
            table.removeColumn(i);
        }
    }

    private void createColumns(CSVData  data, ColumnSortEvent.ListHandler<CSVEntry> sortHandler) {
        if (data.columns() == null) {
            logger.warning("CSV data has no columns assigned.");
            return;
        }
        for (final Enum col : data.columns()) {
            if (col == null) {
                logger.warning("NULL column detected.");
                continue;
            }

            TextColumn<CSVEntry> column = new TextColumn<CSVEntry>() {
                @Override
                public String getValue(CSVEntry entry) {
                    String val = entry.getValue(col);

                    // Report blank for missing or null-like values to display nicely
                    if (val == null || val.equals("-1")) {
                        return "";
                    }

                    return val;
                }
            };
            column.setSortable(true);
            table.addColumn(column, col.toString());

            sortHandler.setComparator(column, new Comparator<CSVEntry>() {
                @Override
                public int compare(CSVEntry o1, CSVEntry o2) {
                    String val1 = o1.getValue(col);
                    String val2 = o2.getValue(col);

                    if (val1.matches(NUM_REGEX) && val2.matches(NUM_REGEX)) {
                        return Integer.parseInt(val1) - Integer.parseInt(val2);
                    }

                    return o1.getValue(col).compareToIgnoreCase(o2.getValue(col));
                }
            });
        }
    }
}
