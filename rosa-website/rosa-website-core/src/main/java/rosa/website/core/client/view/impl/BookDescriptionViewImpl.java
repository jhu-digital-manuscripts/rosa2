package rosa.website.core.client.view.impl;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import rosa.website.core.client.view.BookDescriptionView;

public class BookDescriptionViewImpl extends Composite implements BookDescriptionView {

    public BookDescriptionViewImpl() {
        SimplePanel root = new SimplePanel();

        initWidget(root);
    }
}
