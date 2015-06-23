package rosa.website.core.client.view;

import com.google.gwt.user.client.ui.IsWidget;

public interface HTMLView extends IsWidget {
    void setHTML(String html);

    /**
     * Clear contents of view.
     */
    void clear();
}
