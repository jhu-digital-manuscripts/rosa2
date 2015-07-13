package rosa.website.search.client.widget;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLTable.Cell;
import rosa.search.model.QueryOperation;
import rosa.website.model.select.BookInfo;
import rosa.website.search.client.SearchCategory;

/**
 * AdvancedSearchWidget lets a user build a complex search query through the UI.
 * Query fragments can have individual search field restrictions and are composed
 * with boolean operations. The entire complex search query can have its results
 * restricted to a set list of books.
 */
public class AdvancedSearchWidget extends Composite {
    private static final int INITIAL_OFFSET = 0;

    private final FlexTable queriesTable;

    private final Button searchButton;
    private final Button addFieldButton;
    private final BookPickerWidget bookRestrictionWidget;

    private final ClickHandler removeButtonClickHandler;
    private final KeyPressHandler searchBoxKeyPressHandler;

    private String removeButtonText;
    private QueryOperation[] queryOperations;
    private SearchCategory[] queryFields;

    /**
     * Create a new AdvancedSearchWidget
     */
    public AdvancedSearchWidget() {
        FlowPanel root = new FlowPanel();

        this.queriesTable = new FlexTable();
        this.searchButton = new Button();
        this.addFieldButton = new Button();
        this.bookRestrictionWidget = new BookPickerWidget();

        this.removeButtonClickHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                Cell source = queriesTable.getCellForEvent(event);
                if (source != null) {
                    queriesTable.removeRow(source.getRowIndex());
                }
            }
        };

        this.searchBoxKeyPressHandler = new KeyPressHandler() {
            @Override
            public void onKeyPress(KeyPressEvent event) {
                if (event.getCharCode() == KeyCodes.KEY_ENTER) {
                    searchButton.click();
                }
            }
        };

        root.add(queriesTable);
        root.add(addFieldButton);
        root.add(bookRestrictionWidget);
        root.add(searchButton);

        bind();

        initWidget(root);
    }

    private void bind() {
        addFieldButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                addQueryField();
            }
        });
    }

    /**
     * Clear all data from this widget.
     */
    public void clear() {
        queryOperations = null;
        queryFields = null;
        queriesTable.clear();
    }

    public void setAvailableOperations(QueryOperation[] ops) {
        this.queryOperations = ops;
    }

    public void setAvailableFields(SearchCategory[] fields) {
        this.queryFields = fields;
    }

    /**
     * Add a new query row to this widget allowing for another search query
     * fragment that has its own restrictions that will be composed with all
     * other fragments visible in this widget.
     *
     * @param initialTerm search term to appear in the text box, if applicable
     * @param selectedOperation index of selected operation in {@param availableOperations},
     *                          or -1 if nothing is selected
     * @param selectedField index of selected field in {@param availableFields}, or -1 if
     *                      nothing is selected
     */
    public void addQueryField(String initialTerm, int selectedOperation, int selectedField) {
        AdvancedQueryFragmentWidget row = new AdvancedQueryFragmentWidget();
        queriesTable.setWidget(queriesTable.getRowCount(), 0, row);

        // Add operations
        row.setOperations(queryOperations);
        if (selectedOperation != -1) {
            row.setSelectedOperation(selectedOperation);
        }
        // Add restriction fields
        row.setSearchCategories(queryFields);
        if (selectedField != -1) {
            row.setSelectedSearchCategory(selectedField);
        }
        // Set initial search term, if applicable
        row.setSearchTerm(initialTerm);

        row.addClickRemoveHandler(removeButtonClickHandler);
        row.addKeyPressHandler(searchBoxKeyPressHandler);
        row.setRemoveButtonText(removeButtonText);

        row.setFocus(true);
    }

    /**
     * Add an empty query field.
     */
    public void addQueryField() {
        addQueryField("", -1, -1);
    }

    /**
     * Set the display text for the button that will add a new query row.
     *
     * @param text .
     */
    public void setAddFieldButtonText(String text) {
        addFieldButton.setText(text);
    }

    /**
     * Set the display text for the button that will start the search.
     *
     * @param text .
     */
    public void setSearchButtonText(String text) {
        searchButton.setText(text);
    }

    public void setClearBooksButtonText(String text) {
        bookRestrictionWidget.setClearButtonText(text);
    }

    public void setRemoveButtonText(String text) {
        this.removeButtonText = text;
    }

    /**
     * Add an array of books to the book restriction list. During a search,
     * any results will be restricted to only those books on this list.
     *
     * @param books book names
     */
    public void addBooksToRestrictionList(BookInfo... books) {
        bookRestrictionWidget.addBooks(books);
    }

    /**
     * Define the behavior when the search button is pressed.
     *
     * @param handler click handler
     * @return .
     */
    public HandlerRegistration addSearchButtonClickHandler(ClickHandler handler) {
        return searchButton.addClickHandler(handler);
    }

    public String getSearchToken() {
        return buildSearchToken();
    }

    private String buildSearchToken() {
        StringBuilder query = new StringBuilder();

        for (int i = 0; i < queriesTable.getRowCount(); i++) {
            if (!(queriesTable.getWidget(i, 0) instanceof AdvancedQueryFragmentWidget)) {
                continue;
            }

            AdvancedQueryFragmentWidget row = (AdvancedQueryFragmentWidget) queriesTable.getWidget(i, 0);

            if (isNotBlank(row.getSearchTerm()) && row.getCategory() != null) {
                query.append(row.getCategory());
                query.append(';');
                query.append(row.getSearchTerm());
                query.append(';');
            }
        }

        // If no search terms were entered, dump out early
        if (isBlank(query.toString())) {
            return null;
        }

        String[] books = bookRestrictionWidget.getRestrictedBookIds();
        if (books != null && books.length > 0) {
            query.append("BOOK;");
            for (String book : books) {
                if (isBlank(book)) {
                    continue;
                }

                query.append(book);
                query.append(';');
            }
        }

        query.append(INITIAL_OFFSET);

        return query.toString();
    }

    private boolean isBlank(String val) {
        return val == null || val.isEmpty();
    }

    private boolean isNotBlank(String val) {
        return !isBlank(val);
    }

    // TODO build search query, use search model/API
    // TODO build history token from query
}
