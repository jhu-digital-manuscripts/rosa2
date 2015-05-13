package rosa.website.core.client.view;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.RequiresResize;
import rosa.website.model.select.BookSelectList;

public interface BookSelectView extends IsWidget, RequiresResize {
    public interface Presenter {  }

    void setData(BookSelectList data);
}
