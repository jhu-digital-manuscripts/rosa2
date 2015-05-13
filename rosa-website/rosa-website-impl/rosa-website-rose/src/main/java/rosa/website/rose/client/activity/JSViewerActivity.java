package rosa.website.rose.client.activity;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import rosa.archive.model.Book;
import rosa.archive.model.BookImage;
import rosa.website.core.client.ArchiveDataServiceAsync;
import rosa.website.core.client.ClientFactory;
import rosa.website.core.client.event.BookSelectEvent;
import rosa.website.core.client.jsviewer.codexview.CodexController;
import rosa.website.core.client.jsviewer.codexview.CodexController.ChangeHandler;
import rosa.website.core.client.jsviewer.codexview.CodexImage;
import rosa.website.core.client.jsviewer.codexview.CodexModel;
import rosa.website.core.client.jsviewer.codexview.CodexOpening;
import rosa.website.core.client.jsviewer.codexview.CodexView.Mode;
import rosa.website.core.client.jsviewer.codexview.RoseBook;
import rosa.website.core.client.jsviewer.codexview.SimpleCodexController;
import rosa.website.core.client.jsviewer.dynimg.FsiImageServer;
import rosa.website.core.client.jsviewer.dynimg.ImageServer;
import rosa.website.core.client.place.BookViewerPlace;
import rosa.website.core.client.view.JSViewerView;
import rosa.website.rose.client.WebsiteConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JSViewerActivity implements Activity {
    private static final Logger logger = Logger.getLogger(JSViewerActivity.class.toString());

    private Map<String, String> fsi_share = new HashMap<>();  // TODO configure this using 'fsi-share-map.properties'
    private List<HandlerRegistration> handlers;

    private JSViewerView view;
    private ArchiveDataServiceAsync archiveService;
    private final com.google.web.bindery.event.shared.EventBus eventBus;

    private String collection;
    private String book;
    private String lang;
    private Book b;

    private Mode viewerMode;

    private int current_selected_index;

    public JSViewerActivity(BookViewerPlace place, ClientFactory clientFactory) {
        this.view = clientFactory.jsViewerView();
        this.archiveService = clientFactory.archiveDataService();
        this.eventBus = clientFactory.eventBus();
        this.book = place.getBook();
        this.collection = clientFactory.context().getCollection();
        this.lang = clientFactory.context().getLanguage();
        this.viewerMode = getViewerMode(place.getType());
        this.handlers = new ArrayList<>();

        getBook(book);

        fsi_share.put("rosecollection", "rose");
        fsi_share.put("pizancollection", "pizan");
        fsi_share.put("aorcollection", "aor");

        current_selected_index = 0;
    }

    @Override
    public String mayStop() {
        return null;
    }

    @Override
    public void onCancel() {
        this.eventBus.fireEvent(new BookSelectEvent(false, book));
    }

    @Override
    public void onStop() {
        view.clear();

        for (HandlerRegistration registration : handlers) {
            registration.removeHandler();
        }
        handlers.clear();
        this.eventBus.fireEvent(new BookSelectEvent(false, book));
    }

    @Override
    public void start(AcceptsOneWidget panel, EventBus eventBus) {
        this.eventBus.fireEvent(new BookSelectEvent(true, book));
        panel.setWidget(view);
        final String fsi_missing_image = fsi_share.get(collection) + "/missing_image.tif";

        archiveService.loadImageList(collection, book, new AsyncCallback<String>() {
            @Override
            public void onFailure(Throwable caught) {
                logger.log(Level.SEVERE, "Failed to load image list for book. [" + collection + ":" + book + "]",
                        caught);
            }

            @Override
            public void onSuccess(String result) {
                RoseBook roseBook = new RoseBook(fsi_share.get(collection), result, fsi_missing_image);
                setupView(roseBook.model());
            }
        });

        archiveService.loadPermissionStatement(collection, book, lang, new AsyncCallback<String>() {
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
        logger.info("Setting up the viewer. (MODE:" + viewerMode + ")");
        final CodexController controller = new SimpleCodexController(model);
        ImageServer server = new FsiImageServer(WebsiteConfig.INSTANCE.fsiUrl());

        view.setCodexView(server, model, controller, viewerMode);

        handlers.add(view.addFirstClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                controller.gotoOpening(model.opening(0));
            }
        }));

        handlers.add(view.addLastClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                controller.gotoOpening(model.opening(model.numOpenings() - 1));
            }
        }));

        handlers.add(view.addNextClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                controller.gotoNextOpening();
            }
        }));

        handlers.add(view.addPrevClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                controller.gotoPreviousOpening();
            }
        }));

        handlers.add(view.addGoToKeyDownHandler(new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent event) {
                if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                    int index = getImageIndex(view.getGotoText(), b);

                    /*
                        This hack gets around a bug in the original website where a user inputs
                        a verso page into the 'goto' text box and hits enter. The resulting
                        opening will be the previous opening to what the user intends. Adding one to
                        the index at this stage will boost the index to the facing recto and
                        avoid the issue.

                        This is likely because of the indexing scheme of the openings. Since the front
                        cover will start the index at 0, any subsequent verso page will fall on an odd
                        number. The integer rounding of the index to get the opening index means that
                        the guessed opening index will be rounded down to the previous opening as opposed
                        to the intended opening. Even though this should work for all of the Rosa books,
                        this will not work in the general case, as it will depend on the number of
                        single images that appear at the front of the book, before recto/verso numbering
                        occurs.
                     */
                    if (view.getGotoText().endsWith("v") || view.getGotoText().endsWith("V")) {
                        index++;
                    }

                    if (index != -1) {
                        index /= 2;

                        if (index < model.numOpenings()) {
                            controller.gotoOpening(model.opening(index));
                        }
                    }
                }
            }
        }));

        controller.addChangeHandler(new ChangeHandler() {
            @Override
            public void openingChanged(CodexOpening opening) {
                view.setToolbarVisible(true);

                current_selected_index = opening.position() * 2;
                view.setGotoText(opening.label());
            }

            @Override
            public void viewChanged(List<CodexImage> viewList) {
                view.setToolbarVisible(false);
                if (viewList.size() > 0) {
                    CodexImage img = viewList.get(0);
                    current_selected_index = model.findOpeningImage(img.id());
                }
            }
        });

        switch (viewerMode) {
            case PAGE_TURNER:
                int index = current_selected_index / 2;

                if (index < model.numOpenings()) {
                    controller.gotoOpening(model.opening(index));
                } else {
                    controller.gotoOpening(model.opening(0));
                }

                break;
            case IMAGE_BROWSER:
                break;
            case IMAGE_VIEWER:
                if (current_selected_index < model.numImages()) {
                    controller.setView(model.image(current_selected_index));
                } else {
                    controller.setView(model.nonOpeningImage(current_selected_index - model.numImages()));
                }

                break;
            default:
                view.setToolbarVisible(false);
                break;
        }
    }

    private Mode getViewerMode(String type) {
        switch (type) {
            case "read":
                return Mode.PAGE_TURNER;
            case "browse":
                return Mode.IMAGE_BROWSER;
            default:
                return null;
        }
    }

    private void getBook(String name) {
        archiveService.loadBook(collection, name, new AsyncCallback<Book>() {
            @Override
            public void onFailure(Throwable caught) {
                logger.log(Level.SEVERE, "Failed to load book. [" + book + "]", caught);
            }

            @Override
            public void onSuccess(Book result) {
                b = result;
            }
        });
    }

    private int getImageIndex(String name, Book book) {
        if (book != null && book.getImages() != null && book.getImages().getImages() != null) {
            for (int i = 0; i < book.getImages().getImages().size(); i++) {
                BookImage image = book.getImages().getImages().get(i);
                if (image.getName().equals(name)) {
                    return i;
                }
            }
        }

        return -1;
    }
}
