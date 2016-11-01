package rosa.website.core.client.view;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;

import rosa.website.model.table.Table;

import java.util.Map;

public interface CSVDataView extends ErrorWidget {

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
    void setData(Table data);

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
     * {@link rosa.website.core.client.widget.CSVWidget#createColumns(Table, ListHandler, Map, String...)}
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
    void setData(Table data, Map<Enum<?>, String> links, String[] headers);

    void setDescription(String description);

    /**
     * Add a link separate from the description HTML. This link will be placed between
     * the description and the data table.
     *
     * @param label human readable label to be displayed to user
     * @param target target URL
     * @param downloadFileName OPTIONAL file name if link to be used for download
     */
    void addLink(String label, String target, String downloadFileName);

    /**
     * Add an interactive link for an action that does not necessarily have a target
     * URL. For example, add a link that lets a user download some data from the server
     * using the GWT RPC service.
     *
     * @param label human readable label to be displayed to user
     * @param handler handles mouse click events on this link
     * @return handler registration object
     */
    HandlerRegistration addLink(String label, ClickHandler handler);
}
