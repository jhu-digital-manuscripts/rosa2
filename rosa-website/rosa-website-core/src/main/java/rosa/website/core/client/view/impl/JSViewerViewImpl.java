package rosa.website.core.client.view.impl;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import rosa.website.core.client.jsviewer.codexview.CodexController;
import rosa.website.core.client.jsviewer.codexview.CodexModel;
import rosa.website.core.client.jsviewer.codexview.CodexView;
import rosa.website.core.client.jsviewer.codexview.CodexView.Mode;
import rosa.website.core.client.jsviewer.dynimg.ImageServer;
import rosa.website.core.client.view.JSViewerView;

public class JSViewerViewImpl extends Composite implements JSViewerView {

    private FlowPanel root;
    private FlowPanel readerToolbar;
    private SimplePanel permissionPanel;

    private Button first;
    private Button last;
    private Button prev;
    private Button next;
    private TextBox goTo;

    private CodexView codexView;

    public JSViewerViewImpl() {
        root = new FlowPanel();

        readerToolbar = new FlowPanel();
        first = new Button("First");
        last = new Button("Last");
        prev = new Button("Previous");
        next = new Button("Next");
        goTo = new TextBox();

        readerToolbar.add(first);
        readerToolbar.add(prev);
        readerToolbar.add(goTo);
        readerToolbar.add(next);
        readerToolbar.add(last);

        root.add(readerToolbar);

        permissionPanel = new SimplePanel();
        root.add(permissionPanel);

        initWidget(root);
    }

    @Override
    public void clear() {
        root.remove(codexView);
    }

    @Override
    public void setPermissionStatement(String permission) {
        permissionPanel.setWidget(new HTML(permission));
    }

    @Override
    public void setCodexView(ImageServer imageServer, CodexModel model, CodexController controller, Mode mode) {
        codexView = new CodexView(imageServer, model, controller, (ScrollPanel) this.getParent());
        root.insert(codexView, 0);
        setViewerMode(mode);
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
}
