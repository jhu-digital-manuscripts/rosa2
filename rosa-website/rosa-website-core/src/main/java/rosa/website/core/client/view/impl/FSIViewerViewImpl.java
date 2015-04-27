package rosa.website.core.client.view.impl;

import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import rosa.website.core.client.view.FSIViewerView;
import rosa.website.core.client.widget.FsiViewer;
import rosa.website.core.client.widget.FsiViewer.FSIPagesCallback;
import rosa.website.core.client.widget.FsiViewer.FSIShowcaseCallback;
import rosa.website.core.client.widget.FsiViewerType;

public class FSIViewerViewImpl extends Composite implements FSIViewerView, RequiresResize {
    private Presenter presenter;

    private Timer resizeTimer = new Timer() {
        @Override
        public void run() {
            doResize();
        }
    };

    private SimplePanel permissionPanel;
    private FsiViewer flashViewer;
    private FlowPanel toolbar;
    // Controls: (page controls) first, last, next, previous, *goto.
    // *Show: transcriptions, transcriptions (lecoy), illustrations descriptions
    private TextBox goTo;
    private ListBox showExtra;

    /** Create a new BookViewerView */
    public FSIViewerViewImpl() {
        FlowPanel root = new FlowPanel();
        permissionPanel = new SimplePanel();
        flashViewer = new FsiViewer();
        toolbar = new FlowPanel();

        goTo = new TextBox();
        goTo.setStylePrimaryName("GoTextBox");
        showExtra = new ListBox(false);

        showExtra.setVisibleItemCount(1);

        root.add(toolbar);
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
    public void onResize() {
        resizeTimer.schedule(100);
    }

    @Override
    public HandlerRegistration addGotoKeyPressHandler(KeyPressHandler handler) {
        return goTo.addKeyPressHandler(handler);
    }

    @Override
    public void setGotoLabel(String label) {
        goTo.setText(label);
    }

    @Override
    public void addShowExtraLabels(String... data) {
        showExtra.clear();

        if (data == null) {
            return;
        }
        for (String str : data) {
            showExtra.addItem(str);
        }
    }

    @Override
    public HandlerRegistration addShowExtraChangeHandler(ChangeHandler handler) {
        return showExtra.addChangeHandler(handler);
    }

    @Override
    public void addPagesToolbar() {
        setupSharedToolbar();
        toolbar.add(showExtra);
    }

    @Override
    public void addShowcaseToolbar() {
        setupSharedToolbar();
    }

    @Override
    public void setupFsiPagesCallback(FSIPagesCallback cb) {
        flashViewer.setupFSIPagesCallback(cb);
    }

    @Override
    public void setupFsiShowcaseCallback(FSIShowcaseCallback cb) {
        flashViewer.setupFSIShowcaseCallback(cb);
    }

    private void doResize() {
        int width = getParent().getOffsetWidth();
        int height = getParent().getOffsetHeight()
                - permissionPanel.getOffsetHeight() - 32    // Subtract constant for top/bottom margins on probable <p> in permissions
                - toolbar.getOffsetHeight();

        flashViewer.resize(width + "px", height + "px");
    }

    /**
     * Shared controls for both FSI flash viewer and JS viewer:
     *   Goto page, show: transcription, transcription(Lecoy), illustration descriptions, etc
     */
    private void setupSharedToolbar() {
        toolbar.add(goTo);
    }
}
