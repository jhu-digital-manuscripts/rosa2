package rosa.website.core.client.view;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.view.client.RangeChangeEvent;
import com.google.gwt.view.client.RangeChangeEvent.Handler;
import rosa.search.model.QueryOperation;
import rosa.website.model.select.BookInfo;
import rosa.website.search.client.model.SearchCategory;
import rosa.website.search.client.model.SearchMatchModel;

import java.util.List;
import java.util.Map;

public interface AdvancedSearchView extends ErrorWidget {

    /**
     * Set the display text for the "add field" button.
     *
     * @param text .
     */
    void setAddFieldButtonText(String text);

    /**
     * Set the display text for the "search" button.
     *
     * @param text .
     */
    void setSearchButtonText(String text);

    /**
     * Set the text for the "clear" button.
     *
     * @param text .
     */
    void setClearBooksButtonText(String text);

    /**
     * Set the text for the "remove" button in each query row.
     *
     * @param text .
     */
    void setRemoveButtonText(String text);

    /**
     * Add books to the list of books
     *
     * @param books .
     */
    void addBooksToRestrictionList(BookInfo... books);

    /**
     * Select these books to restrict search results. The books do not have
     * to be added to the book restriction list beforehand.
     *
     * @param books .
     */
    void setBooksAsRestricted(BookInfo... books);

    /**
     * Set the search fields available in this widget. The user can choose
     * from these fields to restrict query results.
     *
     * @param fields .
     */
    void setAvailableSearchFields(SearchCategory[] fields);

    void setSearchFieldLabels(Map<SearchCategory, String> labels);

    /**
     * Set the operations available in this widget. The user can choose
     * from these operations to define the interaction between query
     * fragments.
     *
     * @param operations .
     */
    void setAvailableSearchOperations(QueryOperation[] operations);

    /**
     * Add an empty query field.
     */
    void addQueryField();

    /**
     * Add a new query field with initial values.
     *
     * @param initialTerm search term to appear in the text box, if applicable
     * @param selectedOperation index of selected operation or -1 if nothing is selected
     * @param selectedField index of selected field or -1 if nothing is selected
     */
    void addQueryField(String initialTerm, int selectedOperation, int selectedField);

    /**
     * Clear all data from this view.
     */
    void clear();

    /**
     * Define the behavior when the search button is clicked.
     *
     * @param handler click handler
     * @return .
     */
    HandlerRegistration addSearchButtonClickHandler(ClickHandler handler);

    String getSearchQuery();

    /**
     * Define behavior of the results list on range change. This handler activates
     * whenever the user changes the display range of the results list by clicking
     * the 'next' button or 'previous' button or selects a particular results page.
     *
     * @param handler .
     */
    HandlerRegistration addRangeChangeHandler(RangeChangeEvent.Handler handler);

    /**
     * Set the visible range of data in the results list. This will trigger a
     * {@link RangeChangeEvent} which will be  handled by the range change handler
     * set in {@link #addRangeChangeHandler(Handler)}.
     *
     * @param start start of range
     * @param length length of range
     */
    void setVisibleRange(int start, int length);

    void setPageSize(int pageSize);

    void setRowData(int start, List<SearchMatchModel> data);

    /**
     * Set the total number of rows the results list will contain.
     *
     * @param count .
     */
    void setRowCount(int count);
}
