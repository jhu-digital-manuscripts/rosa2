package rosa.website.core.client.view.impl;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import rosa.website.core.client.view.AdvancedSearchView;
import rosa.website.core.client.widget.AdvancedSearchWidget;
import rosa.website.model.select.BookInfo;

public class AdvancedSearchViewImpl extends Composite implements AdvancedSearchView {

    private AdvancedSearchWidget searchWidget;

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
        searchWidget.addBooksToResetrictionList(books);
    }

    public void setClearBooksButtonText(String text) {
        searchWidget.setClearBooksButtonText(text);
    }

    @Override
    public void setFakeSearchModel() {
        searchWidget.setAddFieldButtonText("Add Field");
        searchWidget.setSearchButtonText("Search");
        searchWidget.setRemoveButtonText("Remove");

        BookInfo[] books = new BookInfo[10];
        for (int i = 0; i < 10; i++) {
            books[i] = new BookInfo("Book " + i, "Book" + i);
        }
        searchWidget.addBooksToResetrictionList(books);

        String[] availableOps = {"AND", "OR"};
        String[] availableFields = {"Field 1", "Field 2", "Field 3", "Field 4"};
        searchWidget.setAvailableFields(availableFields);
        searchWidget.setAvailableOperations(availableOps);

        searchWidget.addQueryField();
        searchWidget.addQueryField();
    }

    private native void console(String message) /*-{
        console.log(message);
    }-*/;
}
