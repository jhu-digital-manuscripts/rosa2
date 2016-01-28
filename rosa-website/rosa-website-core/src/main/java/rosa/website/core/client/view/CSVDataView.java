package rosa.website.core.client.view;

import com.google.gwt.place.shared.Place;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.client.ui.IsWidget;
import rosa.website.model.csv.CSVData;

import java.util.Map;

public interface CSVDataView extends IsWidget {

    interface Presenter {
        void goTo(Place place);
    }

    /**
     * Clear data.
     */
    void clear();

    void setPresenter(Presenter presenter);

    /**
     * Set data to be displayed as a table.
     *
     * @param data CSV data to display
     */
    void setData(CSVData data);

    /**
     *
     * Set data to be displayed as a table. It is possible for one or more columns
     * in this table to be clickable links to another place in this website. In these
     * cases, the base URL will stay the same, but a new place will be identified in
     * the URL fragment.
     *
     * Note: these links are hyperlinks to a place within this website and will not
     * work if trying to link outside.
     *
     * If links should be displayed in this table, it is possible to identify them in
     * links. This parameter maps a column enumeration to a history fragment
     * prefix, which will point to another place in the website. The final history token
     * will be a combination of the mapped history prefix and the cell value in the
     * column.
     *
     * <em>More behavior must be defined in the CSVWidget, this should change in
     * the future.</em>
     * {@link rosa.website.core.client.widget.CSVWidget#createColumns(CSVData, ListHandler, Map, String...)}
     *
     * EXAMPLE:
     * In the 'collection spreadsheet', the column holding book IDs contains links that
     * will navigate to the description of that book. Along with the CSVData, a map
     * with a single entry is passed as the links parameter.
     * <ul><li>Column.ID -&gt; book</li></ul>
     * This link will navigate to: {@code baseURL#book;<bookId>}
     *
     * @param data CSV data to display
     * @param links possible links in table, Column enum -&gt; history fragment prefix
     * @param headers column headers, optional
     */
    void setData(CSVData data, Map<Enum, String> links, String[] headers);

    void setDescription(String description);
}
