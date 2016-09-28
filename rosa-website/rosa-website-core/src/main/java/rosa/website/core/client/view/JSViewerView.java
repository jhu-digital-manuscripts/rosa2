package rosa.website.core.client.view;

import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;
import rosa.pageturner.client.model.Book;
import rosa.pageturner.client.model.Opening;
import rosa.website.viewer.client.jsviewer.codexview.CodexController;
import rosa.website.viewer.client.jsviewer.codexview.CodexModel;
import rosa.website.viewer.client.jsviewer.codexview.CodexView;
import rosa.website.viewer.client.jsviewer.codexview.CodexView.Mode;
import rosa.website.viewer.client.jsviewer.dynimg.ImageServer;

public interface JSViewerView extends ErrorWidget {
    void setHeader(String header);
    void setPermissionStatement(String permission);

    /**
     * Set a new CodexView to display a book using this built in JavaScript
     * viewer. If a CodexView already exists in this view, it will be
     * overwritten.
     *
     * @param imageServer API for making calls to the image server
     * @param model data model for this CodexView
     * @param controller CodexView controller
     * @param mode page turner or browser mode?
     */
    void setCodexView(ImageServer imageServer, CodexModel model, CodexController controller, Mode mode);
    void setFsiJS(Book model);
    void setViewerSize(String width, String height);
    void setViewerMode(CodexView.Mode mode);
    void setToolbarVisible(boolean visible);
    void setGotoText(String text);
    void setOpening(Opening opening);
    String getGotoText();

    void setShowExtraLabels(String ... data);
    HandlerRegistration addShowExtraChangeHandler(ChangeHandler handler);
    HandlerRegistration addOpeningChangeHandler(ValueChangeHandler<Opening> handler);
    void setSelectedShowExtra(String selected);
    String getSelectedShowExtra();
    void showExtra(Widget widget);

    /**
     * Set the behavior of clicks on the 'first' button.
     *
     * @param handler click handler
     * @return handler registration for later de-registration of this handler
     */
    HandlerRegistration addFirstClickHandler(ClickHandler handler);

    /**
     * Set the behavior of clicks on the 'last' button.
     *
     * @param handler click handler
     * @return handler registration for later de-registration of this handler
     */
    HandlerRegistration addLastClickHandler(ClickHandler handler);

    /**
     * Set the behavior of clicks on the 'next' button.
     *
     * @param handler click handler
     * @return handler registration for later de-registration of this handler
     */
    HandlerRegistration addNextClickHandler(ClickHandler handler);

    /**
     * Set the behavior of clicks on the 'prev' button.
     *
     * @param handler click handler
     * @return handler registration for later de-registration of this handler
     */
    HandlerRegistration addPrevClickHandler(ClickHandler handler);

    /**
     * Set the behavior of keyboard events on the 'goto page' text box.
     *
     * @param handler click handler
     * @return handler registration for later de-registration of this handler
     */
    HandlerRegistration addGoToKeyDownHandler(KeyDownHandler handler);

    void onResize();
    void setResizable(boolean resizable);
}
