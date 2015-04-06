package rosa.website.rose.client.activity;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import rosa.archive.model.Book;
import rosa.archive.model.BookImage;
import rosa.archive.model.BookMetadata;
import rosa.website.core.client.ArchiveDataServiceAsync;
import rosa.website.core.client.ClientFactory;
import rosa.website.core.client.place.BookDescriptionPlace;
import rosa.website.core.client.view.BookDescriptionView;
import rosa.website.rose.client.WebsiteConfig;

import java.util.logging.Level;
import java.util.logging.Logger;

public class BookDescriptionActivity implements Activity, BookDescriptionView.Presenter {
    private static final Logger logger = Logger.getLogger(BookDescriptionActivity.class.toString());

    private final String bookName;
    private final ArchiveDataServiceAsync service;
    private final BookDescriptionView view;

    private Book book;

    public BookDescriptionActivity(BookDescriptionPlace place, ClientFactory clientFactory) {
        this.bookName = place.getBook();
        this.service = clientFactory.archiveDataService();
        this.view = clientFactory.bookDescriptionView();
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
        logger.info("Start book description activity. (" + bookName + ")");
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

    private void handleData(Book book) {
        this.book = book;

        view.setMetadata(book.getBookMetadata("en"));
        view.setDescription(book.getBookDescription("en"));
    }

    @Override
    public String getPageUrl(String page) {
        if (book == null) {
            return null;
        }

        for (BookImage image : book.getImages()) {
            if (image.getName().equals(page)) {
                return GWT.getHostPageBaseURL() + "#read;" + image.getId();
            }
        }

        return null;
    }
}
