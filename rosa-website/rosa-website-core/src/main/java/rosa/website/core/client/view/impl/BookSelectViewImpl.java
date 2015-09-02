package rosa.website.core.client.view.impl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.CellBrowser;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwt.view.client.TreeViewModel;
import rosa.website.core.client.Labels;
import rosa.website.core.client.view.BookSelectView;
import rosa.website.core.client.widget.BookSelectionBrowserResources;
import rosa.website.core.client.widget.BookSelectionTreeViewModel;
import rosa.website.model.select.BookInfo;
import rosa.website.model.select.BookSelectList;

public class BookSelectViewImpl extends Composite implements BookSelectView {
    private static final int DEFAULT_WIDTH = 600;       // pixels

    private SimplePanel selectionPanel;
    private Label header;

    /**  */
    public BookSelectViewImpl() {
        FlowPanel root = new FlowPanel();
        selectionPanel = new SimplePanel();
        header = new Label(Labels.INSTANCE.selectBook());

        header.setWidth("100%");
        header.addStyleName("ContentTitle");

        root.add(header);
        root.add(selectionPanel);

        root.setSize("100%", "100%");

        initWidget(root);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setData(BookSelectList data) {
        selectionPanel.clear();

        TreeViewModel browserModel = new BookSelectionTreeViewModel(data, data.getCategory(),
                new SingleSelectionModel<BookInfo>());

        BookSelectionBrowserResources css = GWT.create(BookSelectionBrowserResources.class);
        CellBrowser browser = new CellBrowser.Builder(browserModel, null) // this builder constructor uses unchecked operation
                .pageSize(Integer.MAX_VALUE)                    // Set page size absurdly high to prevent paging
                .resources(css)
                .build();
        browser.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.BOUND_TO_SELECTION);
        browser.setSize("100%", "100%");

        int parent_width = getParent() == null ? DEFAULT_WIDTH : getParent().getOffsetWidth() - 30;
        browser.setDefaultColumnWidth(parent_width / 2);

        selectionPanel.setWidget(browser);
    }

    @Override
    public void setHeaderText(String text) {
        header.setText(text);
    }

    /**
     * Set the size of this view.
     *
     * @param width units included
     * @param height units included
     */
    public void resize(String width, String height) {
        selectionPanel.setSize(width, height);
    }

    @Override
    public void onResize() {
        int width = this.getParent().getOffsetWidth() - 50;
        int height = this.getParent().getOffsetHeight() - header.getOffsetHeight() - 40;

        resize(width + "px", height + "px");
    }
}
