package rosa.website.core.client.view.impl;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import rosa.website.core.client.view.BookViewerView;
import rosa.website.core.client.widget.FsiViewer;
import rosa.website.core.client.widget.FsiViewerType;

public class BookViewerViewImpl extends Composite implements BookViewerView, RequiresResize {

    private Timer resizeTimer = new Timer() {
        @Override
        public void run() {
            doResize();
        }
    };

    private SimplePanel permissionPanel;
    private FsiViewer flashViewer;

    /** Create a new BookViewerView */
    public BookViewerViewImpl() {
        FlowPanel root = new FlowPanel();
        permissionPanel = new SimplePanel();
        flashViewer = new FsiViewer();

        root.add(flashViewer);
        root.add(permissionPanel);
        root.setSize("100%", "100%");

        initWidget(root);
    }

    @Override
    public void setPermissionStatement(String perm) {
        permissionPanel.setWidget(new HTML(perm));
    }

    @Override
    public void setFlashViewer(String html, FsiViewerType type) {
        flashViewer.setHtml(html, type);
        doResize();
    }

    @Override
    public void useFlash(boolean useFlash) {
        flashViewer.setVisible(useFlash);
        // jsViewer.setVisible(!useFlash);
    }

    @Override
    public void onResize() {
        resizeTimer.schedule(100);
    }

    private void doResize() {
        int width = getParent().getOffsetWidth();
        // Subtract constant (30) for top/bottom margins on probable <p> in permissions
        int height = getParent().getOffsetHeight() - permissionPanel.getOffsetHeight() - 30;

        flashViewer.resize(width + "px", height + "px");
    }
}
