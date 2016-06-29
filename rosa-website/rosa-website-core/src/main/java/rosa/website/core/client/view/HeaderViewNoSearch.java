package rosa.website.core.client.view;

import com.google.gwt.user.client.ui.IsWidget;

import java.util.Map;

public interface HeaderViewNoSearch extends IsWidget {
    interface Presenter {
        /**
         * Tell the presenter to go to the "home" place.
         */
        void goHome();
    }

    void setPresenter(Presenter presenter);

    /**
     * Add an image to the header.
     *
     * @param imageUrl URL of desired image
     * @param altText alt text of image
     */
    void addHeaderImage(String imageUrl, String altText);

    /**
     * Add a link to the header.
     *
     * @param label human readable text to display to the user
     * @param target target URL fragment
     */
    void addNavLink(String label, String target);

    /**
     * Add a new item to the navigation bar in this header. The new
     * item will pop up a sub menu populated by the subMenu map.
     *
     * The subMenu map contains a mapping of menu item labels to URL
     * fragments.
     *
     * @param topLabel text to diplay to the reader. Selecting this label will
     *                 pop up the sub menu.
     * @param subMenuMap Map of labels to URL fragments with which to populate the
     *                sub menu.
     */
    void addNavMenu(String topLabel, Map<String, String> subMenuMap);
}
