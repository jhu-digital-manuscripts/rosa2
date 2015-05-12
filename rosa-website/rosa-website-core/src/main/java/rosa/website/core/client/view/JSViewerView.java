package rosa.website.core.client.view;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.IsWidget;
import rosa.website.core.client.jsviewer.codexview.CodexController;
import rosa.website.core.client.jsviewer.codexview.CodexModel;
import rosa.website.core.client.jsviewer.codexview.CodexView;
import rosa.website.core.client.jsviewer.codexview.CodexView.Mode;
import rosa.website.core.client.jsviewer.dynimg.ImageServer;

public interface JSViewerView extends IsWidget {
    void clear();
    void setPermissionStatement(String permission);
    void setCodexView(ImageServer imageServer, CodexModel model, CodexController controller, Mode mode);
    void setViewerMode(CodexView.Mode mode);
    void setToolbarVisible(boolean visible);
    void setGotoText(String text);
    String getGotoText();
    HandlerRegistration addFirstClickHandler(ClickHandler handler);
    HandlerRegistration addLastClickHandler(ClickHandler handler);
    HandlerRegistration addNextClickHandler(ClickHandler handler);
    HandlerRegistration addPrevClickHandler(ClickHandler handler);
    HandlerRegistration addGoToKeyDownHandler(KeyDownHandler handler);
}
