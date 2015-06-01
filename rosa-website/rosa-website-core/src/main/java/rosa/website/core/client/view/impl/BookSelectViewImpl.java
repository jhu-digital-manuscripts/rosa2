package rosa.website.core.client.view.impl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.CellBrowser;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwt.view.client.TreeViewModel;
import rosa.website.core.client.view.BookSelectView;
import rosa.website.core.client.widget.BookSelectionBrowserResources;
import rosa.website.core.client.widget.BookSelectionTreeViewModel;
import rosa.website.model.select.BookInfo;
import rosa.website.model.select.BookSelectList;

public class BookSelectViewImpl extends Composite implements BookSelectView {

    private SimplePanel root;

    /**  */
    public BookSelectViewImpl() {
        root = new SimplePanel();

        root.setSize("100%", "100%");

        initWidget(root);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setData(BookSelectList data) {
        root.clear();

        TreeViewModel browserModel = new BookSelectionTreeViewModel(data, data.getCategory(),
                new SingleSelectionModel<BookInfo>());

        BookSelectionBrowserResources css = GWT.create(BookSelectionBrowserResources.class);
        CellBrowser browser = new CellBrowser.Builder(browserModel, null) // this builder constructor uses unchecked operation
//                .loadingIndicator(null)
                .resources(css)
                .build();

        browser.setSize("100%", "100%");

        root.setWidget(browser);
    }

    /**
     * Set the size of this view.
     *
     * @param width units included
     * @param height units included
     */
    public void resize(String width, String height) {
        root.setSize(width, height);
    }

    @Override
    public void onResize() {
        int width = this.getParent().getOffsetWidth();
        int height = this.getParent().getOffsetHeight();

        resize(width + "px", height + "px");
    }
}
