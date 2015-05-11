package rosa.website.rose.client.activity;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import rosa.website.core.client.ArchiveDataServiceAsync;
import rosa.website.core.client.ClientFactory;
import rosa.website.core.client.jsviewer.codexview.CodexController;
import rosa.website.core.client.jsviewer.codexview.CodexModel;
import rosa.website.core.client.jsviewer.codexview.CodexView.Mode;
import rosa.website.core.client.jsviewer.codexview.RoseBook;
import rosa.website.core.client.jsviewer.codexview.SimpleCodexController;
import rosa.website.core.client.jsviewer.dynimg.FsiImageServer;
import rosa.website.core.client.jsviewer.dynimg.ImageServer;
import rosa.website.core.client.place.FSIViewerPlace;
import rosa.website.core.client.view.JSViewerView;
import rosa.website.rose.client.WebsiteConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JSViewerActivity implements Activity {
    private static final Logger logger = Logger.getLogger(JSViewerActivity.class.toString());

    private Map<String, String> fsi_share = new HashMap<>();  // TODO configure this using 'fsi-share-map.properties'


    private JSViewerView view;
    private ArchiveDataServiceAsync archiveService;

    private String collection;
    private String book;

    public JSViewerActivity(FSIViewerPlace place, ClientFactory clientFactory) {
        this.view = clientFactory.jsViewerView();
        this.archiveService = clientFactory.archiveDataService();
        this.book = place.getBook();
        this.collection = clientFactory.context().getCollection();

        fsi_share.put("rosecollection", "rose");
        fsi_share.put("pizancollection", "pizan");
        fsi_share.put("aorcollection", "aor");
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

        archiveService.loadImageList(collection, book, new AsyncCallback<String>() {
            @Override
            public void onFailure(Throwable caught) {
                logger.log(Level.SEVERE, "Failed to load image list for book. [" + collection + ":" + book + "]",
                        caught);
            }

            @Override
            public void onSuccess(String result) {
                RoseBook roseBook = new RoseBook(fsi_share.get(collection), result);
                setupView(roseBook.model());
            }
        });

        archiveService.loadPermissionStatement(collection, book, "en", new AsyncCallback<String>() {
            @Override
            public void onFailure(Throwable caught) {
                logger.log(Level.SEVERE, "Failed to load permission statement. [" + collection + ":" + book + "]",
                        caught);
            }

            @Override
            public void onSuccess(String result) {
                view.setPermissionStatement(result);
            }
        });
    }

    private void setupView(final CodexModel model) {
        final CodexController controller = new SimpleCodexController(model);
        ImageServer server = new FsiImageServer(WebsiteConfig.INSTANCE.fsiUrl());

        view.setCodexView(server, model, controller);
        view.setViewerMode(Mode.PAGE_TURNER);

        view.addFirstClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                controller.gotoOpening(model.opening(0));
            }
        });

        view.addLastClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                controller.gotoOpening(model.opening(model.numOpenings() - 1));
            }
        });

        view.addNextClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                controller.gotoNextOpening();
            }
        });

        view.addPrevClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                controller.gotoPreviousOpening();
            }
        });
    }
}
