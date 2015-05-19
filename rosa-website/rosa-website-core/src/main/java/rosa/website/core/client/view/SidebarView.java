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

    void addSection(String title, Map<String, String> links);

    void setBookLinks(String title, Map<String, String> links);

    void clearBookLinks();

    void resize(String width, String height);

}
