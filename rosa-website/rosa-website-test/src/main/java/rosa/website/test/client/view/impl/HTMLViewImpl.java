package rosa.website.test.client.view.impl;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ScrollPanel;
import rosa.website.test.client.view.HTMLView;

public class HTMLViewImpl extends Composite implements HTMLView {

    private ScrollPanel root;

    public HTMLViewImpl() {
        this.root = new ScrollPanel();
        root.setSize("100%", "100%");

        initWidget(root);
    }

    @Override
    public void setHTML(String html) {
        clear();
        root.add(new HTML(html));
    }

    @Override
    public void clear() {
        if (root.getWidget() != null) {
            root.clear();
        }
    }
}
