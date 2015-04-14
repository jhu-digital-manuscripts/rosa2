package rosa.website.rose.client.activity;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import rosa.website.core.client.ArchiveDataServiceAsync;
import rosa.website.core.client.ClientFactory;
import rosa.website.core.client.place.BrowseBookPlace;
import rosa.website.core.client.view.BrowseBookView;
import rosa.website.core.client.widget.FsiViewerHTMLBuilder;
import rosa.website.core.client.widget.FsiViewerType;
import rosa.website.rose.client.WebsiteConfig;

import java.util.logging.Level;
import java.util.logging.Logger;

public class BrowseBookActivity implements Activity {
    private static final Logger logger = Logger.getLogger(BrowseBookActivity.class.toString());

// TODO need an application context for 'useFlash' state and language
    private boolean useFlash;
    private String book;
    private BrowseBookView view;
    private ArchiveDataServiceAsync service;

    private int current_selected_image = 1;

    public BrowseBookActivity(BrowseBookPlace place, ClientFactory clientFactory) {
        this.useFlash = place.useFlash();
        this.book = place.getBook();
        this.view = clientFactory.browseBookView();
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

        String collection = WebsiteConfig.INSTANCE.collection();
        String fsi_xml_url = GWT.getModuleBaseURL() + "fsi/"
                + collection
                + "/" + book
                + "/showcase.fsi";
        logger.info("Using FSI XML: [" + fsi_xml_url + "]");

        String fsiHtml = new FsiViewerHTMLBuilder()
                .book(collection, book, "en")
                .type(FsiViewerType.SHOWCASE)
                .fsiBookData(URL.encode(fsi_xml_url))
                .build();

        view.setFlashViewer(fsiHtml);

        service.loadPermissionStatement(collection, book, "en", new AsyncCallback<String>() {
            @Override
            public void onFailure(Throwable caught) {
                logger.log(Level.SEVERE, "Failed to load permission statement.", caught);
            }

            @Override
            public void onSuccess(String result) {
                view.setPermissionStatement(result);
            }
        });

        view.useFlash(true);
    }
}
