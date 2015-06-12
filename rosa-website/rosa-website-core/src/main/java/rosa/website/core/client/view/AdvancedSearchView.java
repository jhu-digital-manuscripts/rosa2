package rosa.website.core.client.view;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.IsWidget;
import rosa.website.model.select.BookInfo;

public interface AdvancedSearchView extends IsWidget {

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
     * Set the search fields available in this widget. The user can choose
     * from these fields to restrict query results.
     *
     * @param fields .
     */
    void setAvailableSearchFields(String[] fields);

    /**
     * Set the operations available in this widget. The user can choose
     * from these operations to define the interaction between query
     * fragments.
     *
     * @param operations .
     */
    void setAvailableSearchOperations(String[] operations);

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
}
