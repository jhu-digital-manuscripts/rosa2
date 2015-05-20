package rosa.website.core.client.view;

import com.google.gwt.user.client.ui.IsWidget;

public interface HeaderView extends IsWidget {
    interface Presenter {
        void goHome();
    }

    void setPresenter(Presenter presenter);
}
