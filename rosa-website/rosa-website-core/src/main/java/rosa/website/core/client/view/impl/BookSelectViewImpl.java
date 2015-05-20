package rosa.website.core.client.view.impl;

import com.google.gwt.user.cellview.client.CellBrowser;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwt.view.client.TreeViewModel;
import rosa.website.core.client.view.BookSelectView;
import rosa.website.core.client.widget.BookSelectionTreeViewModel;
import rosa.website.model.select.BookInfo;
import rosa.website.model.select.BookSelectList;

public class BookSelectViewImpl extends Composite implements BookSelectView {

    private SimplePanel root;

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

        CellBrowser browser = new CellBrowser.Builder(browserModel, null) // this builder constructor uses unchecked operation
//                .loadingIndicator(null)
                .build();

        browser.setSize("100%", "100%");



        root.setWidget(browser);
    }

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
