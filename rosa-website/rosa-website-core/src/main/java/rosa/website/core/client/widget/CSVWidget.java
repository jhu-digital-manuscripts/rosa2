package rosa.website.core.client.widget;

import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.view.client.ListDataProvider;
import rosa.website.core.client.place.BookDescriptionPlace;
import rosa.website.core.client.view.CSVDataView.Presenter;
import rosa.website.model.csv.CSVData;
import rosa.website.model.csv.CSVRow;

import java.util.Comparator;
import java.util.Map;
import java.util.logging.Logger;

public class CSVWidget extends Composite {
    private static final Logger logger = Logger.getLogger(CSVWidget.class.toString());
    private static final String NUM_REGEX = "^-?\\d+(\\.\\d+)?$";

    private final CellTable<CSVRow> table;
    private final ListDataProvider<CSVRow> dataProvider;

    private Presenter presenter;

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

    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
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
        createColumns(data, sortHandler, null);

        table.addColumnSortHandler(sortHandler);
        table.setPageSize(data.size());
        table.flush();
    }

    /**
     * Set the CSV data to be displayed. Also force hyperlinks in the data by specifying
     * columns and the target history token to be linked from that column.
     *
     * Example: say you want the ID column data to link to the "book" place in the app.
     * The links map would contain the mapping ID -&gt; book. The ID column data would
     * then link out to this place, initializing it with the column data.
     *
     * @param data data
     * @param links links, force a column of data to be hyperlinked to place in the app
     */
    @SuppressWarnings("unchecked")
    public void setData(CSVData data, Map<Enum, String> links) {
        clear();

        dataProvider.setList(data.asList());
        ListHandler<CSVRow> sortHandler = new ListHandler<CSVRow>(dataProvider.getList()) {
            @Override
            public void onColumnSort(ColumnSortEvent event) {
                super.onColumnSort(event);
                dataProvider.refresh();
            }
        };
        createColumns(data, sortHandler, links);

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

    private void createColumns(CSVData  data, ColumnSortEvent.ListHandler<CSVRow> sortHandler,
                               final Map<Enum, String> links) {
        if (data.columns() == null) {
            logger.warning("CSV data has no columns assigned.");
            return;
        }
        for (final Enum col : data.columns()) {
            if (col == null) {
                logger.warning("NULL column detected.");
                continue;
            }

            com.google.gwt.user.cellview.client.Column<CSVRow, String> column;
            if (links != null && links.containsKey(col)) {
                column = new com.google.gwt.user.cellview.client.Column<CSVRow, String>(new ClickableTextCell()) {
                    @Override
                    public String getValue(CSVRow val) {
                        return val.getValue(col);
                    }
                };
                column.setFieldUpdater(new FieldUpdater<CSVRow, String>() {
                    @Override
                    public void update(int index, CSVRow object, String value) {
                        switch (links.get(col)) {       // TODO this kind of sucks, since it needs to know about site implementation
                            case "book":
                                presenter.goTo(new BookDescriptionPlace(object.getValue(col)));
                                break;
                            default:
                                break;
                        }
                    }
                });
                column.setCellStyleNames("link");
            } else {
                column = new TextColumn<CSVRow>() {
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
            }

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
