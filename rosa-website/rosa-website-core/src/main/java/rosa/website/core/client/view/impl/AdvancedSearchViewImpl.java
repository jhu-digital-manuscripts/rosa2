package rosa.website.core.client.view.impl;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import rosa.website.core.client.view.AdvancedSearchView;
import rosa.website.core.client.widget.AdvancedSearchWidget;
import rosa.website.model.select.BookInfo;

public class AdvancedSearchViewImpl extends Composite implements AdvancedSearchView {

    private AdvancedSearchWidget searchWidget;

    /**  */
    public AdvancedSearchViewImpl() {
        FlowPanel root = new FlowPanel();

        this.searchWidget = new AdvancedSearchWidget();

        root.add(searchWidget);

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
    public void setClearBooksButtonText(String text) {
        searchWidget.setClearBooksButtonText(text);
    }

    @Override
    public void setRemoveButtonText(String text) {
        searchWidget.setRemoveButtonText(text);
    }

    public void setAvailableSearchFields(String[] fields) {
        searchWidget.setAvailableFields(fields);
    }

    public void setAvailableSearchOperations(String[] operations) {
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

    // TODO add search results stuff
}
