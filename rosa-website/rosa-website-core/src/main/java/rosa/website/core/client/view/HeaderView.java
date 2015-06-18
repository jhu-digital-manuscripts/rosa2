package rosa.website.core.client.view;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.IsWidget;

public interface HeaderView extends IsWidget {
    interface Presenter {
        void goHome();
    }

    void setPresenter(Presenter presenter);
    void addHeaderImage(String imageUrl, String altText);
    void setSearchButtonText(String text);
    HandlerRegistration addSearchClickHandler(ClickHandler handler);
    void addAdvancedSearchLink(String displayText, String targetHistoryToken);
    String getSearchToken();
}
