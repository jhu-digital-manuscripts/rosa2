package rosa.website.core.client.view;

import com.google.gwt.user.client.ui.IsWidget;

import java.util.Map;

public interface SidebarView extends IsWidget {

    interface Presenter extends IsWidget {
        void setUseFlash(boolean useFlash);
        String getCurrentToken();
    }

    void setPresenter(Presenter presenter);

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

}
