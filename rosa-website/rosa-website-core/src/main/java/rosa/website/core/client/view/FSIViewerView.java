package rosa.website.core.client.view;

import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.IsWidget;
import rosa.website.core.client.widget.FsiViewerType;

public interface FSIViewerView extends IsWidget {

    interface Presenter {
        String[] getExtraDataLabels(String page);
    }

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
    void setFlashViewer(String html, FsiViewerType type);

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
    HandlerRegistration addGotoKeyPressHandler(KeyPressHandler handler);

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
     * Add values to the 'Show extra data' control.
     *
     * @param data labels to add
     */
    void addShowExtraLabels(String... data);

    /**
     * Show the toolbar for the FSI pages (page turner) viewer.
     */
    void addPagesToolbar();

    /**
     * Show the toolbar for the FSI showcase viewer.
     */
    void addShowcaseToolbar();
}
