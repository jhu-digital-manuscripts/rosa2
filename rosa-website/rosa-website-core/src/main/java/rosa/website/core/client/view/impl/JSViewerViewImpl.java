package rosa.website.core.client.view.impl;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import rosa.website.core.client.Labels;
import rosa.website.core.client.widget.ViewerControlsWidget;
import rosa.website.viewer.client.jsviewer.codexview.CodexController;
import rosa.website.viewer.client.jsviewer.codexview.CodexModel;
import rosa.website.viewer.client.jsviewer.codexview.CodexView;
import rosa.website.viewer.client.jsviewer.codexview.CodexView.Mode;
import rosa.website.viewer.client.jsviewer.dynimg.ImageServer;
import rosa.website.core.client.view.JSViewerView;

public class JSViewerViewImpl extends Composite implements JSViewerView, RequiresResize {
    private FlowPanel root;
    private FlowPanel readerToolbar;
    private SimplePanel permissionPanel;
    private SimplePanel transcriptionPanel;

    private Button first;
    private Button last;
    private Button prev;
    private Button next;
    private TextBox goTo;

    private ViewerControlsWidget viewerControlsWidget;

    private CodexView codexView;

    private Timer resizeTimer = new Timer() {
        @Override
        public void run() {
            doResize();
        }
    };

    /**  */
    public JSViewerViewImpl() {
        root = new FlowPanel();

        transcriptionPanel = new SimplePanel();
        root.add(transcriptionPanel);

        readerToolbar = new FlowPanel();

        Labels labels = Labels.INSTANCE;
        first = new Button(labels.first());
        last = new Button(labels.last());
        prev = new Button(labels.previous());
        next = new Button(labels.next());
        goTo = new TextBox();
        viewerControlsWidget = new ViewerControlsWidget();

        viewerControlsWidget.setGoToVisible(false);

        readerToolbar.add(first);
        readerToolbar.add(prev);
        readerToolbar.add(goTo);
        readerToolbar.add(next);
        readerToolbar.add(last);
        readerToolbar.add(viewerControlsWidget);

        root.add(readerToolbar);

        permissionPanel = new SimplePanel();
        root.add(permissionPanel);

        initWidget(root);
    }

    @Override
    public void setPermissionStatement(String permission) {
        permissionPanel.setWidget(new HTML(permission));
    }

    @Override
    public void setCodexView(ImageServer imageServer, CodexModel model, CodexController controller, Mode mode) {
        if (codexView != null) {
            codexView.removeFromParent();
        }

        codexView = new CodexView(imageServer, model, controller, (ScrollPanel) this.getParent());
        root.insert(codexView, 0);
        setViewerMode(mode);

        Scheduler.get().scheduleDeferred(new ScheduledCommand() {
            @Override
            public void execute() {
                doResize();
            }
        });
    }

    @Override
    public void setViewerMode(Mode mode) {
        codexView.setMode(mode);
        if (mode == Mode.PAGE_TURNER) {
            readerToolbar.setVisible(true);
        } else {
            readerToolbar.setVisible(false);
        }
    }

    @Override
    public void setToolbarVisible(boolean visible) {
        readerToolbar.setVisible(visible);
    }

    @Override
    public void setGotoText(String text) {
        goTo.setText(text);
    }

    @Override
    public String getGotoText() {
        return goTo.getText();
    }

    @Override
    public void setShowExtraLabels(String... data) {
        viewerControlsWidget.setShowExtraLabels(data);
    }

    @Override
    public HandlerRegistration addShowExtraChangeHandler(ChangeHandler handler) {
        return viewerControlsWidget.addShowExtraChangeHandler(handler);
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
    public void showExtra(Widget widget) {
        if (widget == null) {
            transcriptionPanel.clear();
            transcriptionPanel.setVisible(false);
        } else {
            transcriptionPanel.setWidget(widget);
            transcriptionPanel.setVisible(true);
        }
    }

    @Override
    public HandlerRegistration addFirstClickHandler(ClickHandler handler) {
        return first.addClickHandler(handler);
    }

    @Override
    public HandlerRegistration addLastClickHandler(ClickHandler handler) {
        return last.addClickHandler(handler);
    }

    @Override
    public HandlerRegistration addNextClickHandler(ClickHandler handler) {
        return next.addClickHandler(handler);
    }

    @Override
    public HandlerRegistration addPrevClickHandler(ClickHandler handler) {
        return prev.addClickHandler(handler);
    }

    @Override
    public HandlerRegistration addGoToKeyDownHandler(KeyDownHandler handler) {
        return goTo.addKeyDownHandler(handler);
    }

    @Override
    public void onResize() {
        resizeTimer.schedule(100);
    }

    private void doResize() {   // TODO take image aspect ratio into account, so image isn't squished!
        int width = getOffsetWidth() - 30
                - (transcriptionPanel.isVisible() ? transcriptionPanel.getOffsetWidth() : 0);
        int height = getOffsetHeight() - 30
                - permissionPanel.getOffsetHeight()
                - readerToolbar.getOffsetHeight();

        codexView.setSize(width + "px", height + "px");
    }
}
