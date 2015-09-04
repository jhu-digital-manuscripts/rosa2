package rosa.website.core.client.view.impl;

import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import rosa.website.core.client.view.FSIViewerView;
import rosa.website.core.client.widget.ViewerControlsWidget;
import rosa.website.viewer.client.fsiviewer.FSIViewer;
import rosa.website.viewer.client.fsiviewer.FSIViewer.FSIPagesCallback;
import rosa.website.viewer.client.fsiviewer.FSIViewer.FSIShowcaseCallback;
import rosa.website.viewer.client.fsiviewer.FSIViewerType;

public class FSIViewerViewImpl extends Composite implements FSIViewerView, RequiresResize {

    private Timer resizeTimer = new Timer() {
        @Override
        public void run() {
            doResize();
        }
    };

    private Label header;
    private SimplePanel permissionPanel;
    private FSIViewer flashViewer;
    private SimplePanel transcriptionPanel;
    private ViewerControlsWidget viewerControlsWidget;

    /** Create a new BookViewerView */
    public FSIViewerViewImpl() {
        FlowPanel root = new FlowPanel();
        header = new Label();
        permissionPanel = new SimplePanel();
        flashViewer = new FSIViewer();
        transcriptionPanel = new SimplePanel();
        viewerControlsWidget = new ViewerControlsWidget();

        transcriptionPanel.setStylePrimaryName("Transcription");
        header.setStylePrimaryName("ContentTitle");
        header.setWidth("100%");

        root.add(header);
        root.add(viewerControlsWidget);
        root.add(flashViewer);
        root.add(transcriptionPanel);
        root.add(permissionPanel);
        root.setSize("100%", "100%");

        permissionPanel.addStyleName("float-left");

        initWidget(root);
    }

    @Override
    public void setHeader(String text) {
        header.setText(text);
    }

    @Override
    public void setPermissionStatement(String perm) {
        permissionPanel.setWidget(new HTML(perm));
    }

    @Override
    public void setFlashViewer(String html, FSIViewerType type) {
        flashViewer.setHtml(html, type);
        doResize();
    }

    @Override
    public void onResize() {
        resizeTimer.schedule(100);
    }

    @Override
    public HandlerRegistration addGotoKeyDownHandler(KeyDownHandler handler) {
        return viewerControlsWidget.addGotoKeyDownHandler(handler);
    }

    @Override
    public void setGotoLabel(String label) {
        viewerControlsWidget.setGotoLabel(label);
    }

    @Override
    public String getGotoText() {
        return viewerControlsWidget.getGotoText();
    }

    @Override
    public void setShowExtraLabels(String... data) {
        viewerControlsWidget.setShowExtraLabels(data);
    }

    @Override
    public void setSelectedShowExtra(String selected) {
        viewerControlsWidget.setSelectedShowExtra(selected);
    }

    @Override
    public String getSelectedShowExtra() {
        return viewerControlsWidget.getSelected();
    }

    @Override
    public HandlerRegistration addShowExtraChangeHandler(ChangeHandler handler) {
        return viewerControlsWidget.addShowExtraChangeHandler(handler);
    }

    @Override
    public void addPagesToolbar() {
        setupSharedToolbar();
        viewerControlsWidget.setShowExtraVisible(true);
    }

    @Override
    public void addShowcaseToolbar() {
        setupSharedToolbar();
        viewerControlsWidget.setShowExtraVisible(false);
    }

    @Override
    public void setupFsiPagesCallback(FSIPagesCallback cb) {
        flashViewer.setupFSIPagesCallback(cb);
    }

    @Override
    public void setupFsiShowcaseCallback(FSIShowcaseCallback cb) {
        flashViewer.setupFSIShowcaseCallback(cb);
    }

    @Override
    public void fsiViewerGotoImage(int image) {
        flashViewer.fsiViewerGoToImage(image);
    }

    @Override
    public void fsiViewerSelectImage(int image) {
        flashViewer.fsiSelectImage(image);
    }

    @Override
    public void showExtra(Widget widget) {
        if (widget == null) {
            transcriptionPanel.clear();
            transcriptionPanel.setVisible(false);
        } else {
            transcriptionPanel.setWidget(widget);
            transcriptionPanel.setVisible(true);
        }
    }

    private void doResize() {
        if (getParent() == null || permissionPanel == null || viewerControlsWidget == null) {
            return;
        }

        int width = getParent().getOffsetWidth() - 100
                - (transcriptionPanel.isVisible() ? transcriptionPanel.getOffsetWidth() : 0);
        int height = getParent().getOffsetHeight()
                - permissionPanel.getOffsetHeight() - 32    // Subtract constant for top/bottom margins on probable <p> in permissions
                - viewerControlsWidget.getOffsetHeight();

        flashViewer.resize(width + "px", height + "px");
    }

    /**
     * Shared controls for both FSI flash viewer pages and showcase view
     *   Goto page, show: transcription, transcription(Lecoy), illustration descriptions, etc
     */
    private void setupSharedToolbar() {
        viewerControlsWidget.setVisible(true);
    }
}
