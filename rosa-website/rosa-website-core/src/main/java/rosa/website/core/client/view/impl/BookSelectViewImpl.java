package rosa.website.core.client.view.impl;

import com.google.gwt.user.cellview.client.CellBrowser;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import rosa.website.core.client.view.BookSelectView;

public class BookSelectViewImpl extends Composite implements BookSelectView {




    private SimplePanel root;
    private CellBrowser browser;



    public BookSelectViewImpl() {
        root = new SimplePanel();



        initWidget(root);
    }

}
