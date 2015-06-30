package rosa.website.core.client.view;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.IsWidget;

public interface HeaderView extends IsWidget {
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

    void setSearchButtonText(String text);

    /**
     * Add a click handler to the "search" button in the header to define
     * its behavior.
     *
     * @param handler click handler
     * @return .
     */
    HandlerRegistration addSearchClickHandler(ClickHandler handler);

    /**
     * Add a link to "advanced search" in the header.
     *
     * @param displayText text to display to user
     * @param targetHistoryToken target history token
     */
    void addAdvancedSearchLink(String displayText, String targetHistoryToken);

    String getSearchToken();
}
