package rosa.website.rose.client.activity;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import rosa.website.core.client.ArchiveDataServiceAsync;
import rosa.website.core.client.ClientFactory;
import rosa.website.core.client.place.BookSelectPlace;
import rosa.website.core.client.view.BookSelectView;
import rosa.website.core.client.widget.LoadingPanel;
import rosa.website.model.select.BookSelectList;
import rosa.website.model.select.SelectCategory;
import rosa.website.rose.client.WebsiteConfig;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Activity for selecting books by sorting through criteria.
 */
public class BookSelectActivity implements Activity {
    private static final Logger logger = Logger.getLogger(BookSelectActivity.class.toString());

    private final BookSelectView view;
    private final SelectCategory category;

    private final ArchiveDataServiceAsync service;

    /**
     * Create a new BookSelectActivity
     *
     * @param place initial state of activity
     * @param clientFactory .
     */
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
        LoadingPanel.INSTANCE.hide();
    }

    @Override
    public void onStop() {
        LoadingPanel.INSTANCE.hide();
    }

    @Override
    public void start(final AcceptsOneWidget panel, EventBus eventBus) {
        panel.setWidget(view);
        LoadingPanel.INSTANCE.show();

        service.loadBookSelectionData(
                WebsiteConfig.INSTANCE.collection(),
                category,
                LocaleInfo.getCurrentLocale().getLocaleName(),
                new AsyncCallback<BookSelectList>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        logger.log(Level.SEVERE, "Failed to load book selection data.", caught);
                        LoadingPanel.INSTANCE.hide();
                    }

                    @Override
                    public void onSuccess(BookSelectList result) {
                        LoadingPanel.INSTANCE.hide();
                        result.setCategory(category);
                        view.setData(result);
                    }
                }
        );

        Scheduler.get().scheduleDeferred(new ScheduledCommand() {
            @Override
            public void execute() {
                view.onResize();
            }
        });
    }
}
