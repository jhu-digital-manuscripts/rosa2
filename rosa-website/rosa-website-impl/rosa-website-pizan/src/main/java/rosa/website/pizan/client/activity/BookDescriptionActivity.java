package rosa.website.pizan.client.activity;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import rosa.archive.model.Book;
import rosa.archive.model.BookImage;
import rosa.website.core.client.ArchiveDataServiceAsync;
import rosa.website.core.client.ClientFactory;
import rosa.website.core.client.event.BookSelectEvent;
import rosa.website.core.client.place.BookDescriptionPlace;
import rosa.website.core.client.view.BookDescriptionView;
import rosa.website.pizan.client.WebsiteConfig;

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

    private Book book;

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
    public void start(AcceptsOneWidget panel, EventBus eventBus) {
        this.eventBus.fireEvent(new BookSelectEvent(true, bookName));
        panel.setWidget(view);

        view.setPresenter(this);
        service.loadBook(WebsiteConfig.INSTANCE.collection(), bookName, new AsyncCallback<Book>() {
            @Override
            public void onFailure(Throwable caught) {
                logger.log(Level.SEVERE, "Failed to load book with the archive data service.", caught);
            }

            @Override
            public void onSuccess(Book result) {
                handleData(result);
            }
        });
    }

    @Override
    public String getPageUrlFragment(String page) {
        if (book == null) {
            return null;
        }

        for (BookImage image : book.getImages()) {
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

    private void handleData(Book book) {
        this.book = book;

        view.setMetadata(book.getBookMetadata(language));
        view.setDescription(book.getBookDescription(language));
    }

    private void finishActivity() {
        view.clear();
        eventBus.fireEvent(new BookSelectEvent(false, bookName));
    }
}
