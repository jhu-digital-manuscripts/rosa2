package rosa.website.core.client.view.impl;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.view.client.RangeChangeEvent.Handler;
import rosa.search.model.QueryOperation;
import rosa.website.core.client.view.AdvancedSearchView;
import rosa.website.core.client.view.ErrorComposite;
import rosa.website.model.select.BookInfo;
import rosa.website.search.client.model.SearchCategory;
import rosa.website.search.client.model.SearchMatchModel;
import rosa.website.search.client.widget.AdvancedSearchWidget;
import rosa.website.search.client.widget.SearchResultsWidget;

import java.util.List;
import java.util.Map;

public class AdvancedSearchViewImpl extends ErrorComposite implements AdvancedSearchView {
    private int thumbWidth = 100;
    private int thumbHeight = 100;

    private AdvancedSearchWidget searchWidget;
    private SearchResultsWidget searchResults;

    /**  */
    public AdvancedSearchViewImpl() {
        FlowPanel root = new FlowPanel();

        this.searchWidget = new AdvancedSearchWidget();
        this.searchResults = new SearchResultsWidget();

        root.add(searchWidget);
        root.add(searchResults);

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

    @Override
    public void setAvailableSearchFields(SearchCategory[] fields) {
        searchWidget.setAvailableFields(fields);
    }

    @Override
    public void setSearchFieldLabels(Map<SearchCategory, String> labels) {
        searchWidget.setSearchFieldLabels(labels);
    }

    @Override
    public void setAvailableSearchOperations(QueryOperation[] operations) {
        searchWidget.setAvailableOperations(operations);
    }

    @Override
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
        searchResults.clear();
    }

    @Override
    public HandlerRegistration addSearchButtonClickHandler(ClickHandler handler) {
        return searchWidget.addSearchButtonClickHandler(handler);
    }

    @Override
    public String getSearchQuery() {
        return searchWidget.getSearchToken();
    }

    @Override
    public HandlerRegistration addRangeChangeHandler(Handler handler) {
        return searchResults.addRangeChangeHandler(handler);
    }

    @Override
    public void setVisibleRange(int start, int length) {
        searchResults.setVisibleRange(start, length);
    }

    @Override
    public void setPageSize(int pageSize) {
        searchResults.setPageSize(pageSize);
    }

    @Override
    public void setRowData(int start, List<SearchMatchModel> data) {
        searchResults.setRowData(start, data);
    }

    @Override
    public void setRowCount(int count) {
        searchResults.setRowCount(count);
    }

}
