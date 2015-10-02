package rosa.website.core.client.view;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.RequiresResize;
import rosa.website.model.select.BookSelectList;

public interface BookSelectView extends IsWidget, RequiresResize {

    interface Presenter {
        void goToDescription(String id);
    }

    void setData(BookSelectList data);
    void setHeaderText(String text);
    void setPresenter(Presenter presenter);
}
