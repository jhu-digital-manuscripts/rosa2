package rosa.website.search.client.widget;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.web.bindery.event.shared.HandlerRegistration;
import rosa.search.model.QueryOperation;
import rosa.website.search.client.model.SearchCategory;

import java.util.Map;

/**
 * Represents the UI element of a single search query fragment. Multiple
 * AdvancedQueryWidgets can be combined to create a UI that can build a
 * complex search query, which can be fed to a search service.
 *
 * The query fragment represented in this interface can be combined with
 * others using the boolean operation, while restricting results to the
 * given category.
 */
public class AdvancedQueryFragmentWidget extends Composite {
    private boolean isFirst;

    private Map<SearchCategory, String> searchCategoryLabels;

    private final ListBox operation;
    private final ListBox category;

    private final TextBox term;

    private final Button remove;

    /**  */
    public AdvancedQueryFragmentWidget() {
        this(false);
    }

    /**
     * @param isFirst is this widget the first in a list?
     */
    public AdvancedQueryFragmentWidget(boolean isFirst) {
        this.isFirst = isFirst;

        Grid main = new Grid(1, 4);

        this.operation = new ListBox();
        this.category = new ListBox();
        this.term = new TextBox();
        this.remove = new Button();

        if (!isFirst) {
            main.setWidget(0, 0, operation);
        }
        main.setWidget(0, 1, category);
        main.setWidget(0, 2, term);
        main.setWidget(0, 3, remove);

        initWidget(main);
        operation.setVisible(false);

        category.setSelectedIndex(0);
    }

    /**
     * Set the list of boolean operations that operate on this query.
     *
     * @param operations .
     */
    public void setOperations(QueryOperation[] operations) {
        operation.clear();

        if (operations != null) {
            for (QueryOperation op : operations) {
                operation.addItem(op.toString(), op.toString());
            }
        }
    }

    public void setSelectedOperation(int index) {
        operation.setSelectedIndex(index);
    }

    public void setSelectedOperation(QueryOperation selected) {
        for (int i = 0; i < operation.getItemCount(); i++) {
            if (QueryOperation.valueOf(operation.getItemText(i)).equals(selected)) {
                setSelectedOperation(i);
                break;
            }
        }
    }

    /**
     * Set the list of category restrictions that can be placed on this query fragment.
     *
     * @param searchFields .
     */
    public void setSearchCategories(SearchCategory[] searchFields) {
        category.clear();

        if (searchFields != null) {
            for (SearchCategory f : searchFields) {
                if (searchCategoryLabels == null || !searchCategoryLabels.containsKey(f)) {
                    category.addItem(f.toString());
                } else {
                    category.addItem(searchCategoryLabels.get(f), f.toString());
                }
            }
        }
    }

    public void setSearchCategoryLabels(Map<SearchCategory, String> searchCategoryLabels) {
        this.searchCategoryLabels = searchCategoryLabels;
    }

    public void setSelectedSearchCategory(int index) {
        category.setSelectedIndex(index);
    }

    public void setSelectedSearchCategory(SearchCategory selected) {
        for (int i = 0; i < category.getItemCount(); i++) {
            if (SearchCategory.valueOf(category.getItemText(i)).equals(selected)) {
                setSelectedSearchCategory(i);
                break;
            }
        }
    }

    public void setSearchTerm(String term) {
        this.term.setValue(term, false);
    }

    public void setRemoveButtonText(String text) {
        if (text != null && !text.isEmpty()) {
            remove.setText(text);
        }
    }

    /**
     * @return the query term from the search text box
     */
    public String getSearchTerm() {
        return term.getValue();
    }

    /**
     * @return the boolean operation associated with this query
     */
    public QueryOperation getOperation() {
        return QueryOperation.valueOf(isFirst ? "AND" : operation.getValue(operation.getSelectedIndex()));
    }

    /**
     * @return the category to restrict the search
     */
    public SearchCategory getCategory() {
        return SearchCategory.valueOf(category.getValue(category.getSelectedIndex()));
    }

    /**
     * Add a key press handler to the search text box to handle keyboard presses.
     *
     * Usage example: if focus is on this widget, and the ENTER key is pressed,
     * execute the search
     *
     * @param handler keyboard key press handler
     * @return .
     */
    public HandlerRegistration addKeyPressHandler(KeyPressHandler handler) {
        return term.addKeyPressHandler(handler);
    }

    /**
     * Add a click handler to the REMOVE button associated with this widget.
     *
     * @param handler click handler
     * @return .
     */
    public HandlerRegistration addClickRemoveHandler(ClickHandler handler) {
        return remove.addClickHandler(handler);
    }

    /**
     * @param hasFocus bring focus to this widget?
     */
    public void setFocus(boolean hasFocus) {
        term.setFocus(hasFocus);
    }
}
