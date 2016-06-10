package rosa.website.rose.client.activity;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import rosa.archive.model.BookImage;
import rosa.website.core.client.ArchiveDataServiceAsync;
import rosa.website.core.client.ClientFactory;
import rosa.website.core.client.Labels;
import rosa.website.core.client.event.BookSelectEvent;
import rosa.website.core.client.event.SidebarItemSelectedEvent;
import rosa.website.core.client.place.BookDescriptionPlace;
import rosa.website.core.client.view.BookDescriptionView;
import rosa.website.core.client.widget.LoadingPanel;
import rosa.website.core.shared.RosaConfigurationException;
import rosa.website.model.view.BookDescriptionViewModel;
import rosa.website.rose.client.WebsiteConfig;

import java.util.logging.Level;
import java.util.logging.Logger;

public class BookDescriptionActivity implements Activity, BookDescriptionView.Presenter {
    private static final Logger logger = Logger.getLogger(BookDescriptionActivity.class.toString());

    private final String bookName;
    private final String language;
    private final ArchiveDataServiceAsync service;
    private final BookDescriptionView view;
    // TODO need to keep this eventBus, or use the one on .start()?
    private com.google.web.bindery.event.shared.EventBus eventBus;

    private BookDescriptionViewModel model;

    /**
     * @param place initial state
     * @param clientFactory .
     */
    public BookDescriptionActivity(BookDescriptionPlace place, ClientFactory clientFactory) {
        this.bookName = place.getBook();
        this.language = LocaleInfo.getCurrentLocale().getLocaleName();
        this.service = clientFactory.archiveDataService();
        this.view = clientFactory.bookDescriptionView();
        this.eventBus = clientFactory.eventBus();
    }

    @Override
    public String mayStop() {
        return null;
    }

    @Override
    public void onCancel() {
        finishActivity();
    }

    @Override
    public void onStop() {
        finishActivity();
    }

    @Override
    public void start(AcceptsOneWidget panel, final EventBus eventBus) {
        final String msg = "Failed to load book description. [" + WebsiteConfig.INSTANCE.collection()
                + "," + bookName + "]";

        LoadingPanel.INSTANCE.show();
        view.clearErrors();
        this.eventBus.fireEvent(new BookSelectEvent(true, bookName));
        panel.setWidget(view);
        view.setPresenter(this);

        Scheduler.get().scheduleDeferred(new ScheduledCommand() {
            @Override
            public void execute() {
                eventBus.fireEvent(new SidebarItemSelectedEvent(Labels.INSTANCE.description()));
            }
        });

        service.loadBookDescriptionModel(WebsiteConfig.INSTANCE.collection(), bookName, language,
                new AsyncCallback<BookDescriptionViewModel>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        view.addErrorMessage(msg);
                        logger.log(Level.SEVERE, msg);
                        if (caught instanceof RosaConfigurationException) {
                            view.addErrorMessage(caught.getMessage());
                        }
                        LoadingPanel.INSTANCE.hide();
                    }

                    @Override
                    public void onSuccess(BookDescriptionViewModel result) {
                        model = result;
                        LoadingPanel.INSTANCE.hide();

                        if (result == null) {
                            view.addErrorMessage(msg);
                            logger.log(Level.SEVERE, msg);
                            return;
                        }

                        view.setModel(result);
                    }
                });
    }

    @Override
    public String getPageUrlFragment(String page) {
        if (model == null || model.getImages() == null) {
            logger.warning("No image list found when trying to get image URL fragment.");
            view.addErrorMessage("Could not find image list for this book.");
            return null;
        }
        if (!page.endsWith("r") && !page.endsWith("v")) {
            page += "r";
        }

        for (BookImage image : model.getImages()) {
            if (image.getName().equals(page)) {
                return "read;" + image.getId();
            }
        }

        return null;
    }

    @Override
    public String getPageUrlFragment(int page) {
        return getPageUrlFragment(page + "r");
    }

    private void finishActivity() {
        LoadingPanel.INSTANCE.hide();
        view.clear();
        eventBus.fireEvent(new BookSelectEvent(false, bookName));
    }
}
