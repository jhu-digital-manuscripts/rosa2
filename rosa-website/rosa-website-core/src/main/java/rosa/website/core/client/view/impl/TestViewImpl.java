package rosa.website.core.client.view.impl;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ScrollPanel;
import rosa.website.core.client.view.TestView;

public class TestViewImpl extends Composite implements TestView {
    private ScrollPanel root;

    public TestViewImpl() {
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
