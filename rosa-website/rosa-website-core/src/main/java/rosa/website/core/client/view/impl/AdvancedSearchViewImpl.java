package rosa.website.core.client.view.impl;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import rosa.search.model.QueryOperation;
import rosa.website.core.client.view.AdvancedSearchView;
import rosa.website.model.select.BookInfo;
import rosa.website.search.client.SearchCategory;
import rosa.website.search.client.widget.AdvancedSearchWidget;

public class AdvancedSearchViewImpl extends Composite implements AdvancedSearchView {

    private AdvancedSearchWidget searchWidget;

    /**  */
    public AdvancedSearchViewImpl() {
        FlowPanel root = new FlowPanel();

        this.searchWidget = new AdvancedSearchWidget();

        root.add(searchWidget);

        root.setStylePrimaryName("Search");

        initWidget(root);
    }

    @Override
    public void setAddFieldButtonText(String text) {
        searchWidget.setAddFieldButtonText(text);
    }

    @Override
    public void setSearchButtonText(String text) {
        searchWidget.setSearchButtonText(text);
    }

    @Override
    public void addBooksToRestrictionList(BookInfo ... books) {
        searchWidget.addBooksToRestrictionList(books);
    }

    @Override
    public void setBooksAsRestricted(BookInfo... books) {
        searchWidget.setBooksAsRestricted(books);
    }

    @Override
    public void setClearBooksButtonText(String text) {
        searchWidget.setClearBooksButtonText(text);
    }

    @Override
    public void setRemoveButtonText(String text) {
        searchWidget.setRemoveButtonText(text);
    }

    public void setAvailableSearchFields(SearchCategory[] fields) {
        searchWidget.setAvailableFields(fields);
    }

    public void setAvailableSearchOperations(QueryOperation[] operations) {
        searchWidget.setAvailableOperations(operations);
    }

    public void addQueryField() {
        searchWidget.addQueryField();
    }

    @Override
    public void addQueryField(String initialTerm, int selectedOperation, int selectedField) {
        searchWidget.addQueryField(initialTerm, selectedOperation, selectedField);
    }

    @Override
    public void clear() {
        searchWidget.clear();
    }

    @Override
    public HandlerRegistration addSearchButtonClickHandler(ClickHandler handler) {
        return searchWidget.addSearchButtonClickHandler(handler);
    }

    @Override
    public String getSearchQuery() {
        return searchWidget.getSearchToken();
    }

    // TODO add search results stuff
}
