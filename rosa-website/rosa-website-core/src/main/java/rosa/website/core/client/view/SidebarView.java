package rosa.website.core.client.view;

import com.google.gwt.user.client.ui.IsWidget;
import rosa.website.core.client.event.BookSelectEventHandler;
import rosa.website.core.client.event.FlashStatusChangeEventHandler;
import rosa.website.core.client.event.SidebarItemSelectedEventHandler;

import java.util.Map;

public interface SidebarView extends IsWidget {

    interface Presenter extends IsWidget, BookSelectEventHandler, FlashStatusChangeEventHandler,
            SidebarItemSelectedEventHandler {
        void setUseFlash(boolean useFlash);
        String getCurrentToken();
    }

    void setPresenter(Presenter presenter);

    /**
     * Set a sidebar item as selected by changing its style. Sidebar item
     * is identified using its own text.
     *
     * @param item item text
     */
    void selectItem(String item);

    void addHelpLink(String url);

    /**
     * @param nav_links map defining the LINK NAME to LINK TARGET
     */
    void setSiteNavigationLinks(Map<String, String> nav_links);

    /**
     * Add a section of navigation links to the sidebar.
     *
     * @param title section header
     * @param links map of link text to link fragment
     */
    void addSection(String title, Map<String, String> links);

    /**
     * Add section for navigation to book viewing places.
     *
     * @param title section header
     * @param links map of link text to link fragment
     */
    void setBookLinks(String title, Map<String, String> links);

    /**
     * Remove the section containing navigation links to book viewing places.
     */
    void clearBookLinks();

    /**
     * @param width .
     * @param height .
     */
    void resize(String width, String height);

    /**
     *
     * @param label Label to appear in the UI
     * @param languageCode language code
     */
    void addLanguageLink(String label, final String languageCode);

}
