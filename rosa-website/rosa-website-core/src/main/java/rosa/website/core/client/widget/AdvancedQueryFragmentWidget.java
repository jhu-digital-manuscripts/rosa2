package rosa.website.core.client.widget;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.web.bindery.event.shared.HandlerRegistration;

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

    private final ListBox operation;
    private final ListBox field;

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
        this.field = new ListBox();
        this.term = new TextBox();
        this.remove = new Button();

        if (!isFirst) {
            main.setWidget(0, 0, operation);
        }
        main.setWidget(0, 1, field);
        main.setWidget(0, 2, term);
        main.setWidget(0, 3, remove);

        initWidget(main);

        field.setSelectedIndex(0);
    }

    /**
     * Set the list of boolean operations that operate on this query.
     *
     * @param operations
     */
    public void setOperations(String[] operations) {
        operation.clear();

        if (operations != null) {
            for (String op : operations) {
                operation.addItem(op);
            }
        }
    }

    public void setSelectedOperation(int index) {
        operation.setSelectedIndex(index);
    }

    // TODO change to Enum from search API?
    public void setSelectedOperation(String selected) {
        for (int i = 0; i < operation.getItemCount(); i++) {
            if (operation.getItemText(i).equals(selected)) {
                setSelectedOperation(i);
                break;
            }
        }
    }

    /**
     * Set the list of field restrictions that can be placed on this query fragment.
     *
     * @param searchFields
     */
    public void setSearchFields(String[] searchFields) {
        field.clear();

        if (searchFields != null) {
            for (String f : searchFields) {
                field.addItem(f);
            }
        }
    }

    public void setSelectedSearchField(int index) {
        field.setSelectedIndex(index);
    }

    // TODO change to Enum from search API?
    public void setSelectedSearchField(String selected) {
        for (int i = 0; i < field.getItemCount(); i++) {
            if (field.getItemText(i).equals(selected)) {
                setSelectedSearchField(i);
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
    public String getOperation() {
        return isFirst ? "AND" : operation.getItemText(operation.getSelectedIndex());
    }

    /**
     * @return the field to restrict the search
     */
    public String getField() {
        return field.getItemText(field.getSelectedIndex());
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
