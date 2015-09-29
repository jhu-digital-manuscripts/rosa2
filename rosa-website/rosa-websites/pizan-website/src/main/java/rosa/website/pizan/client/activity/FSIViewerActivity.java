package rosa.website.pizan.client.activity;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.http.client.URL;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import rosa.archive.model.BookImage;
import rosa.website.core.client.ArchiveDataServiceAsync;
import rosa.website.core.client.ClientFactory;
import rosa.website.core.client.Labels;
import rosa.website.core.client.event.BookSelectEvent;
import rosa.website.core.client.event.SidebarItemSelectedEvent;
import rosa.website.core.client.place.BookViewerPlace;
import rosa.website.core.client.place.HTMLPlace;
import rosa.website.core.client.view.FSIViewerView;
import rosa.website.core.client.widget.LoadingPanel;
import rosa.website.viewer.client.fsiviewer.FSIViewer.FSIPagesCallback;
import rosa.website.viewer.client.fsiviewer.FSIViewer.FSIShowcaseCallback;
import rosa.website.viewer.client.fsiviewer.FSIViewerHTMLBuilder;
import rosa.website.viewer.client.fsiviewer.FSIViewerType;
import rosa.website.model.view.FSIViewerModel;
import rosa.website.pizan.client.WebsiteConfig;

import java.util.logging.Level;
import java.util.logging.Logger;

public class FSIViewerActivity implements Activity {

    private static final Logger logger = Logger.getLogger(FSIViewerActivity.class.toString());
    private static final String FSI_URL_PREFIX = GWT.getModuleBaseURL() + "fsi/";

    private String language;

    private String book;
    private String starterPage;
    private FSIViewerType type;

    private FSIViewerView view;
    private ArchiveDataServiceAsync service;
    private PlaceController placeController;
    private final com.google.web.bindery.event.shared.EventBus eventBus;

    private ScheduledCommand resizeCommand = new ScheduledCommand() {
        @Override
        public void execute() {
            view.onResize();
        }
    };

    private FSIViewerModel model;
    private int current_image_index;

    /** Callback for FSI showcase view. */
    private final FSIShowcaseCallback showcaseCallback = new FSIShowcaseCallback() {
        @Override
        public void imageSelected(int image) {
            view.setGotoLabel(getImageName(image));
        }
    };

    /** Callback for FSI pages view. */
    private final FSIPagesCallback pagesCallback = new FSIPagesCallback() {
        @Override
        public void pageChanged(int page) {
            // Update goto box  with label
            current_image_index = page;

            if (page == model.getImages().getImages().size()) {
                view.setGotoLabel(getImageName(page));
            } else {
                StringBuilder sb = new StringBuilder();
                if (page > 0) {
                    sb.append(getImageName(page - 1));
                    sb.append(',');
                }
                sb.append(getImageName(page));

                view.setGotoLabel(sb.toString());
            }
        }

        @Override
        public void imageInfo(String info) {
            current_image_index = getImageIndexFromPagesInfo(info);
            view.setGotoLabel(getImageName(current_image_index));
        }
    };

    /**
     * @param place state info
     * @param clientFactory .
     */
    public FSIViewerActivity(BookViewerPlace place, ClientFactory clientFactory) {
        this.language = LocaleInfo.getCurrentLocale().getLocaleName();
        this.service = clientFactory.archiveDataService();
        this.view = clientFactory.bookViewerView();
        this.current_image_index = 0;
        this.starterPage = place.getPage();
        this.book = place.getBook();
        this.type = getViewerType(place.getType());
        this.eventBus = clientFactory.eventBus();
        this.placeController = clientFactory.placeController();
    }

    @Override
    public String mayStop() {
        return null;
    }

    @Override
    public void onCancel() {
        this.eventBus.fireEvent(new BookSelectEvent(false, book));
        LoadingPanel.INSTANCE.hide();
    }

    @Override
    public void onStop() {
        this.eventBus.fireEvent(new BookSelectEvent(false, book));
        LoadingPanel.INSTANCE.hide();
    }

    @Override
    public void start(AcceptsOneWidget panel, EventBus eventBus) {
        LoadingPanel.INSTANCE.show();
        this.eventBus.fireEvent(new BookSelectEvent(true, book));
        panel.setWidget(view);

        service.loadFSIViewerModel(WebsiteConfig.INSTANCE.collection(), book, language,
                new AsyncCallback<FSIViewerModel>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        logger.log(Level.SEVERE, "Failed to load FSI data.");
                        LoadingPanel.INSTANCE.hide();
                    }

                    @Override
                    public void onSuccess(FSIViewerModel result) {
                        if (result == null) {
                            Window.alert("An error has occurred, Book not found. [" + book + "]");
                            placeController.goTo(new HTMLPlace(WebsiteConfig.INSTANCE.defaultPage()));
                            return;
                        }
                        model = result;

                        if (result.getPermission() != null && result.getPermission().getPermission() != null) {
                            view.setPermissionStatement(result.getPermission().getPermission());
                        }
                        Scheduler.get().scheduleDeferred(resizeCommand);

                        if (starterPage != null && !starterPage.isEmpty()) {
                            setupFlashViewer(getImageIndex(starterPage));
                        } else {
                            setupFlashViewer(-1);
                        }
                        LoadingPanel.INSTANCE.hide();
                    }
                });
    }

    public String getCurrentPage() {
        return getImageName(current_image_index);
    }

    private void setupFlashViewer(int startPage) {
        if (startPage == -1) {
            startPage = 0;
        }
        String collection = WebsiteConfig.INSTANCE.collection();
        String fsi_xml_url = FSI_URL_PREFIX + collection + "/" + book + "/" + type.getXmlId();

        String fsiHtml = new FSIViewerHTMLBuilder()
                .book(collection, book, language)
                .type(type)
                .fsiBookData(URL.encode(fsi_xml_url))
                .initialImage(startPage)
                .build();

        view.setFlashViewer(fsiHtml, type);

        if (type == FSIViewerType.SHOWCASE) {
            view.addShowcaseToolbar();
            view.setupFsiShowcaseCallback(showcaseCallback);
            view.setHeader(Labels.INSTANCE.browseImages() + ": " + model.getTitle());

            Scheduler.get().scheduleDeferred(new ScheduledCommand() {
                @Override
                public void execute() {
                    eventBus.fireEvent(new SidebarItemSelectedEvent(Labels.INSTANCE.browseImages()));
                }
            });

            // Show nothing in the "show extra" drop down
            view.setShowExtraLabels((String) null);
//            view.showExtra(null);

            view.addGotoKeyDownHandler(new KeyDownHandler() {
                @Override
                public void onKeyDown(KeyDownEvent event) {
                    if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                        String tryThis = view.getGotoText();
                        if (isNumeric(tryThis)) {
                            tryThis += "r";
                        }

                        int index = getImageIndex(tryThis);
                        if (index >= 0) {
                            view.fsiViewerSelectImage(index);
                        }
                    }
                }
            });
        } else if (type == FSIViewerType.PAGES) {
            view.addPagesToolbar();
            view.setupFsiPagesCallback(pagesCallback);
            view.setHeader(Labels.INSTANCE.pageTurner() + ": " + model.getTitle());

            Scheduler.get().scheduleDeferred(new ScheduledCommand() {
                @Override
                public void execute() {
                    eventBus.fireEvent(new SidebarItemSelectedEvent(Labels.INSTANCE.pageTurner()));
                }
            });

            view.addGotoKeyDownHandler(new KeyDownHandler() {
                @Override
                public void onKeyDown(KeyDownEvent event) {
                    if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                        String tryThis = view.getGotoText();
                        if (isNumeric(tryThis)) {
                            tryThis += "r";
                        }

                        int index = getImageIndex(tryThis);
                        if (index >= 0) {
                            view.fsiViewerGotoImage(index + 1);
                        }
                    }
                }
            });
        }


    }

    /**
     * @param index index of page in the book
     * @return short name of a page by index, if it exists. Empty string, otherwise
     */
    private String getImageName(int index) {
        if (book == null || model.getImages() == null || model.getImages().getImages() == null
                || model.getImages().getImages().size() < index || model.getImages().getImages().get(index) == null) {
            return "";
        }

        return model.getImages().getImages().get(index).getName();
    }

    /**
     * @param name name of image
     * @return index of image in the book, if it exists. -1 otherwise.
     */
    private int getImageIndex(String name) {
        if (book != null && model.getImages() != null && model.getImages().getImages() != null) {
            for (int i = 0; i < model.getImages().getImages().size(); i++) {
                BookImage image = model.getImages().getImages().get(i);
                if (image.getName().equals(name) || image.getId().equals(name)) {
                    return i;
                }
            }
        }

        return -1;
    }

    private FSIViewerType getViewerType(String type) {
        switch (type) {
            case "browse":
                return FSIViewerType.SHOWCASE;
            case "read":
                return FSIViewerType.PAGES;
            case "pages":
                return FSIViewerType.PAGES;
            case "showcase":
                return FSIViewerType.SHOWCASE;
            default:
                return null;
        }
    }

    private int getImageIndexFromShowcaseInfo(String info) {
        String marker = "ImageIndex=";
        int i = info.indexOf(marker);

        if (i == -1) {
            return 0;
        }

        return Integer.parseInt(info.substring(i + marker.length()));
    }

    private int getImageIndexFromPagesInfo(String info) {
        int result = getImageIndexFromShowcaseInfo(info) - 1;

        if (result < 0) {
            result = 0;
        }

        return result;
    }

    private native boolean isNumeric(String str) /*-{
        return !isNaN(str);
    }-*/;
}
