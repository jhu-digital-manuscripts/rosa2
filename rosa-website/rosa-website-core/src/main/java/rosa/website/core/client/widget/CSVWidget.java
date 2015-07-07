package rosa.website.core.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.view.client.ListDataProvider;
import rosa.website.model.csv.CSVData;
import rosa.website.model.csv.CSVRow;

import java.util.Comparator;
import java.util.logging.Logger;

public class CSVWidget extends Composite {
    private static final Logger logger = Logger.getLogger(CSVWidget.class.toString());
    private static final String NUM_REGEX = "^-?\\d+(\\.\\d+)?$";

    private final CellTable<CSVRow> table;
    private final ListDataProvider<CSVRow> dataProvider;

    /**
     * Create a new blank CsvWidget.
     */
    public CSVWidget() {
        SimplePanel root = new SimplePanel();

        CellTable.Resources css = GWT.create(CSVCellTableResources.class);

        this.table = new CellTable<>(Integer.MAX_VALUE, css);
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
        ListHandler<CSVRow> sortHandler = new ListHandler<CSVRow>(dataProvider.getList()) {
            @Override
            public void onColumnSort(ColumnSortEvent event) {
                super.onColumnSort(event);
                dataProvider.refresh();
            }
        };
        createColumns(data, sortHandler);

        table.addColumnSortHandler(sortHandler);
        table.setPageSize(data.size());
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

    private void createColumns(CSVData  data, ColumnSortEvent.ListHandler<CSVRow> sortHandler) {
        if (data.columns() == null) {
            logger.warning("CSV data has no columns assigned.");
            return;
        }
        for (final Enum col : data.columns()) {
            if (col == null) {
                logger.warning("NULL column detected.");
                continue;
            }

            TextColumn<CSVRow> column = new TextColumn<CSVRow>() {
                @Override
                public String getValue(CSVRow entry) {
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

            sortHandler.setComparator(column, new Comparator<CSVRow>() {
                @Override
                public int compare(CSVRow o1, CSVRow o2) {
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
