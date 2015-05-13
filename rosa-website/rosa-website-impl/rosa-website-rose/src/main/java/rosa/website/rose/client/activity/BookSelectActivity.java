package rosa.website.rose.client.activity;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Widget;
import rosa.website.core.client.ArchiveDataServiceAsync;
import rosa.website.core.client.ClientFactory;
import rosa.website.core.client.place.BookSelectPlace;
import rosa.website.core.client.view.BookSelectView;
import rosa.website.model.select.BookSelectList;
import rosa.website.model.select.SelectCategory;
import rosa.website.rose.client.WebsiteConfig;

import java.util.logging.Level;
import java.util.logging.Logger;

public class BookSelectActivity implements Activity {
    private static final Logger logger = Logger.getLogger(BookSelectActivity.class.toString());

    private final BookSelectView view;
    private final SelectCategory category;
    private final String lang;

    private final ArchiveDataServiceAsync service;

    public BookSelectActivity(BookSelectPlace place, ClientFactory clientFactory) {
        this.view = clientFactory.bookSelectView();
        this.category = place.getCategory();
        this.service = clientFactory.archiveDataService();
        this.lang = clientFactory.context().getLanguage();
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
    public void start(final AcceptsOneWidget panel, EventBus eventBus) {
        panel.setWidget(view);

        service.loadBookSelectionData(
                WebsiteConfig.INSTANCE.collection(),
                category,
                lang,
                new AsyncCallback<BookSelectList>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        logger.log(Level.SEVERE, "Failed to load book selection data.", caught);
                    }

                    @Override
                    public void onSuccess(BookSelectList result) {
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
