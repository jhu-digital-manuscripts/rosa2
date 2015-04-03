package rosa.website.core.client.view;

import com.google.gwt.user.client.ui.IsWidget;
import rosa.archive.model.BookMetadata;

public interface BookDescriptionView extends IsWidget{
    interface Presenter {
        /**
         * @param page page short name
         * @return URL to view the page
         */
        String getPageUrl(String page);
    }

    void setMetadata(BookMetadata metadata);
    void setPresenter(Presenter presenter);
}
