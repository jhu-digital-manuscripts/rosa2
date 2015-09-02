package rosa.website.core.client.view;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.RequiresResize;
import rosa.website.model.select.BookSelectList;

public interface BookSelectView extends IsWidget, RequiresResize {

    void setData(BookSelectList data);
    void setHeaderText(String text);
}
