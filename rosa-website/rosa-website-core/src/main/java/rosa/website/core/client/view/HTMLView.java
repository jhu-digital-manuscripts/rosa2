package rosa.website.core.client.view;

import com.google.gwt.user.client.ui.IsWidget;

public interface HTMLView extends IsWidget {
    public interface Presenter {

    }

    void setHTML(String html);

    void clear();
}
