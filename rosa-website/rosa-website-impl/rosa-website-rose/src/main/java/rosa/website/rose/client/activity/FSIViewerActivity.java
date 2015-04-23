package rosa.website.rose.client.activity;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import rosa.archive.model.Book;
import rosa.website.core.client.ArchiveDataServiceAsync;
import rosa.website.core.client.ClientFactory;
import rosa.website.core.client.place.FSIViewerPlace;
import rosa.website.core.client.view.FSIViewerView;
import rosa.website.core.client.widget.FsiViewerHTMLBuilder;
import rosa.website.core.client.widget.FsiViewerType;
import rosa.website.rose.client.WebsiteConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FSIViewerActivity implements Activity, FSIViewerView.Presenter {
    private static final Logger logger = Logger.getLogger(FSIViewerActivity.class.toString());
    private static final String FSI_URL_PREFIX = GWT.getModuleBaseURL() + "fsi/";

    private String language;

    private String book;
    private FsiViewerType type;

    private FSIViewerView view;
    private ArchiveDataServiceAsync service;

    private ScheduledCommand resizeCommand = new ScheduledCommand() {
        @Override
        public void execute() {
            view.onResize();
        }
    };

    private Book b;

    /**
     * @param place state info
     * @param clientFactory .
     */
    public FSIViewerActivity(FSIViewerPlace place, ClientFactory clientFactory) {
        this.language = clientFactory.context().getLanguage();
        this.service = clientFactory.archiveDataService();
        this.view = clientFactory.bookViewerView();

        this.book = place.getBook();
        this.type = getViewerType(place.getType());
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
        logger.info("Activity stopped. (book='" + book + "', type='" + type + "')");
    }

    @Override
    public void start(AcceptsOneWidget panel, EventBus eventBus) {
        panel.setWidget(view);
        setupFlashViewer();
    }

    @Override
    public String[] getExtraDataLabels(String page) {
        if (b == null) {
            return new String[0];
        }

        List<String> labels = new ArrayList<>();
        if (b.getTranscription() != null) { // TODO for page
            labels.add("Transcription");
        }
        // TODO transcription (Lecoy)
        if (b.hasIllustrationTagging(page)) {
            labels.add("Illustration descriptions");
        }
        if (b.hasNarrativeTagging(page)) {
            labels.add("Narrative sections");
        }

        return new String[0];
    }

    private void setupFlashViewer() {
        String collection = WebsiteConfig.INSTANCE.collection();
        String fsi_xml_url = FSI_URL_PREFIX + collection + "/" + book + "/" + type.getXmlId();

        String fsiHtml = new FsiViewerHTMLBuilder()
                .book(collection, book, language)
                .type(type)
                .fsiBookData(URL.encode(fsi_xml_url))
                .build();

        view.setFlashViewer(fsiHtml, type);
        fetchBookData();

        if (type == FsiViewerType.SHOWCASE) {
            view.addShowcaseToolbar();
        } else if (type == FsiViewerType.PAGES) {
            view.addPagesToolbar();
        }
    }

    private void fetchBookData() {
        service.loadBook(WebsiteConfig.INSTANCE.collection(), book, new AsyncCallback<Book>() {
            @Override
            public void onFailure(Throwable caught) {
                logger.log(Level.SEVERE, "Failed to load book.", caught);
            }

            @Override
            public void onSuccess(Book result) {
                b = result;
                view.setPermissionStatement(b.getPermission(language).getPermission());

                // Schedule resize after permission statement is added in order to
                // take its height into account
                Scheduler.get().scheduleDeferred(resizeCommand);
            }
        });
    }

    private FsiViewerType getViewerType(String type) {
        // TODO need a map for relationship: history token -> viewer type   instead of hard coding...
        switch (type) {
            case "browse":
                return FsiViewerType.SHOWCASE;
            case "read":
                return FsiViewerType.PAGES;
            case "pages":
                return FsiViewerType.PAGES;
            case "showcase":
                return FsiViewerType.SHOWCASE;
            default:
                return null;
        }
    }
}
