package rosa.website.core.client.view;

import com.google.gwt.user.client.ui.IsWidget;
import rosa.website.model.select.BookSelectList;

public interface BookSelectView extends IsWidget {
    public interface Presenter {  }

    void setData(BookSelectList data);
}
