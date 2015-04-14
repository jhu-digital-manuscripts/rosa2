package rosa.website.core.client.view.impl;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SimplePanel;
import rosa.website.core.client.view.BrowseBookView;
import rosa.website.core.client.widget.FsiViewer;

public class BrowseBookViewImpl extends Composite implements BrowseBookView {

    private FlowPanel root;
    private SimplePanel permissionPanel;
    private FsiViewer flashViewer;

    public BrowseBookViewImpl() {
        root = new FlowPanel();
        permissionPanel = new SimplePanel();
        flashViewer = new FsiViewer();

        root.add(flashViewer);
        root.add(permissionPanel);

        initWidget(root);
    }

    @Override
    public void setPermissionStatement(String perm) {
        permissionPanel.setWidget(new HTML(perm));
    }

    @Override
    public void setFlashViewer(String html) {
        flashViewer.setHtml(html);
    }

    @Override
    public void useFlash(boolean useFlash) {
        flashViewer.setVisible(useFlash);
        // jsViewer.setVisible(!useFlash);
    }
}
