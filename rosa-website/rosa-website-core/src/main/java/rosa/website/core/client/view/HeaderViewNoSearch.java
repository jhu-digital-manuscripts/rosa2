package rosa.website.core.client.view;

import com.google.gwt.user.client.ui.IsWidget;

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
}
