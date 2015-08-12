package rosa.website.core.client.view;

import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import rosa.website.viewer.client.fsiviewer.FSIViewer.FSIPagesCallback;
import rosa.website.viewer.client.fsiviewer.FSIViewer.FSIShowcaseCallback;
import rosa.website.viewer.client.fsiviewer.FSIViewerType;

public interface FSIViewerView extends IsWidget {

    /**
     * Display permission statement for use of data.
     *
     * @param perm permission statement
     */
    void setPermissionStatement(String perm);

    /**
     * Set the HTML content of the FSI flash viewer.
     *
     * @param html .
     * @param type showcase|pages
     */
    void setFlashViewer(String html, FSIViewerType type);

    /**
     * Called on a window resize event.
     */
    void onResize();

    // Controls
    /**
     * Add a key press handler to the 'Goto page' control.
     *
     * @param handler key press handler
     * @return HandlerRegistration object
     */
    HandlerRegistration addGotoKeyDownHandler(KeyDownHandler handler);

    /**
     * Add a (value) change handler to respond to changes in the selection
     * of the 'Show extra data' control.
     *
     * @param handler (value) change handler
     * @return handler registration object
     */
    HandlerRegistration addShowExtraChangeHandler(ChangeHandler handler);

    /**
     * Set the label to display in the 'Goto page' control.
     *
     * @param label text to display
     */
    void setGotoLabel(String label);

    /**
     * @return text in the Goto text box
     */
    String getGotoText();

    /**
     * Add values to the 'Show extra data' control.
     *
     * @param data labels to add
     */
    void setShowExtraLabels(String... data);

    void setSelectedShowExtra(String selected);

    String getSelectedShowExtra();

    /**
     * Show the toolbar for the FSI pages (page turner) viewer.
     */
    void addPagesToolbar();

    /**
     * Show the toolbar for the FSI showcase viewer.
     */
    void addShowcaseToolbar();

    void setupFsiPagesCallback(FSIPagesCallback cb);

    void setupFsiShowcaseCallback(FSIShowcaseCallback cb);

    /**
     * For use with FSI pages
     * @param image index
     */
    void fsiViewerGotoImage(int image);

    /**
     * For use with FSI showcase
     * @param image index
     */
    void fsiViewerSelectImage(int image);

    void showExtra(Widget widget);
}
