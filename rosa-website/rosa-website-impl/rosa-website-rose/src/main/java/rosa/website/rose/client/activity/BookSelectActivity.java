package rosa.website.rose.client.activity;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwt.view.client.TreeViewModel;
import rosa.website.core.client.ArchiveDataServiceAsync;
import rosa.website.core.client.ClientFactory;
import rosa.website.core.client.place.BookSelectPlace;
import rosa.website.core.client.view.BookSelectView;
import rosa.website.core.client.widget.BookSelectionTreeViewModel;
import rosa.website.model.select.BookInfo;
import rosa.website.model.select.BookSelectList;
import rosa.website.model.select.SelectCategory;
import rosa.website.rose.client.WebsiteConfig;

import java.util.logging.Logger;

public class BookSelectActivity implements Activity {
    private static final Logger logger = Logger.getLogger(BookSelectActivity.class.toString());

    private final BookSelectView view;
    private final SelectCategory category;

    private final ArchiveDataServiceAsync service;

    public BookSelectActivity(BookSelectPlace place, ClientFactory clientFactory) {
        this.view = clientFactory.bookSelectView();
        this.category = place.getCategory();
        this.service = clientFactory.archiveDataService();
    }

    @Override
    public String mayStop() {
        return null;
    }

    @Override
    public void onCancel() {

    }

    @Override
    public void onStop() {

    }

    @Override
    public void start(AcceptsOneWidget panel, EventBus eventBus) {
        panel.setWidget(view);

        service.loadBookSelectionData(
                WebsiteConfig.INSTANCE.collection(),
                category,
                "en",
                new AsyncCallback<BookSelectList>() {
                    @Override
                    public void onFailure(Throwable caught) {

                    }

                    @Override
                    public void onSuccess(BookSelectList result) {
                        TreeViewModel browserModel = new BookSelectionTreeViewModel(result, category,
                                new SingleSelectionModel<BookInfo>());
                    }
                }
        );
    }
}
