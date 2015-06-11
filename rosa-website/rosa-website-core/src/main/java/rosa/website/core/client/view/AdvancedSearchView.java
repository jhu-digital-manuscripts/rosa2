package rosa.website.core.client.view;

import com.google.gwt.user.client.ui.IsWidget;
import rosa.website.model.select.BookInfo;

public interface AdvancedSearchView extends IsWidget {
    void setAddFieldButtonText(String text);
    void setSearchButtonText(String text);
    void addBooksToRestrictionList(BookInfo... books);
    void setFakeSearchModel();
}
