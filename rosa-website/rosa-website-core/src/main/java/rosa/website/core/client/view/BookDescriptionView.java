package rosa.website.core.client.view;

import com.google.gwt.user.client.ui.IsWidget;
import rosa.archive.model.BookDescription;
import rosa.archive.model.BookMetadata;
import rosa.website.model.view.BookDescriptionViewModel;

public interface BookDescriptionView extends IsWidget{
    interface Presenter {
        /**
         * @param page page short name
         * @return URL to view the page
         */
        String getPageUrlFragment(String page);

        /**
         * @param page page number (not index!)
         * @return URL to view the page, assume recto of page number
         */
        String getPageUrlFragment(int page);
    }

    /**
     * Clear this view.
     */
    void clear();
    void setModel(BookDescriptionViewModel model);
    void setPresenter(Presenter presenter);
}
