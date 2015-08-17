package rosa.website.rose.client.activity;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import rosa.archive.model.BookImage;
import rosa.archive.model.ImageList;
import rosa.website.core.client.ArchiveDataServiceAsync;
import rosa.website.core.client.ClientFactory;
import rosa.website.core.client.Labels;
import rosa.website.core.client.event.BookSelectEvent;
import rosa.website.core.client.widget.LoadingPanel;
import rosa.website.core.client.widget.TranscriptionViewer;
import rosa.website.model.view.FSIViewerModel;
import rosa.website.viewer.client.jsviewer.codexview.CodexController;
import rosa.website.viewer.client.jsviewer.codexview.CodexController.ChangeHandler;
import rosa.website.viewer.client.jsviewer.codexview.CodexImage;
import rosa.website.viewer.client.jsviewer.codexview.CodexModel;
import rosa.website.viewer.client.jsviewer.codexview.CodexOpening;
import rosa.website.viewer.client.jsviewer.codexview.CodexView.Mode;
import rosa.website.viewer.client.jsviewer.codexview.RoseBook;
import rosa.website.viewer.client.jsviewer.codexview.SimpleCodexController;
import rosa.website.viewer.client.jsviewer.dynimg.FSIImageServer;
import rosa.website.viewer.client.jsviewer.dynimg.ImageServer;
import rosa.website.core.client.place.BookViewerPlace;
import rosa.website.core.client.view.JSViewerView;
import rosa.website.rose.client.WebsiteConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JSViewerActivity implements Activity {

    public enum DisplayCategory {   // TODO move to website model
        NONE(Labels.INSTANCE.show()),
        TRANSCRIPTION(Labels.INSTANCE.transcription()),
        LECOY(Labels.INSTANCE.transcription() + "[" + Labels.INSTANCE.lecoy() + "]"),
        ILLUSTRATION(Labels.INSTANCE.illustrationDescription()),
        NARRATIVE(Labels.INSTANCE.narrativeSections());

        DisplayCategory(String display) {
            this.display = display;
        }

        public final String display;

        public static DisplayCategory category(String label) {
            for (DisplayCategory c : DisplayCategory.values()) {
                if (c.display.equals(label)) {
                    return c;
                }
            }
            return null;
        }
    }

    private static final Logger logger = Logger.getLogger(JSViewerActivity.class.toString());

    private JSViewerView view;
    private ArchiveDataServiceAsync archiveService;
    private final com.google.web.bindery.event.shared.EventBus eventBus;

    private final String collection;
    private final String fsi_share;
    private final String book;
    private final String starterPage;
    // TODO only need ImageList and Permission
    private ImageList images;
    private FSIViewerModel model;

    private Mode viewerMode;
    private DisplayCategory showExtraCategory;

    private int current_selected_index;

    private final com.google.gwt.event.dom.client.ChangeHandler showExtraChangeHandler =
            new com.google.gwt.event.dom.client.ChangeHandler() {
        @Override
        public void onChange(ChangeEvent event) {
            DisplayCategory category = DisplayCategory.category(view.getSelectedShowExtra());
            if (category == null) {
                return;
            }

            showExtraCategory = category;
            handleShowExtra();
        }
    };

    /**
     * Create a new JSViewerActivity. This will setup a new JavaScript viewer
     * instance with the information given in the initial state.
     *
     * @param place initial state
     * @param clientFactory .
     */
    public JSViewerActivity(BookViewerPlace place, ClientFactory clientFactory) {
        this.view = clientFactory.jsViewerView();
        this.archiveService = clientFactory.archiveDataService();
        this.eventBus = clientFactory.eventBus();
        this.book = place.getBook();
        this.collection = clientFactory.context().getCollection();
        this.fsi_share = WebsiteConfig.INSTANCE.fsiShare();
        this.viewerMode = getViewerMode(place.getType());
        this.starterPage = place.getPage();

        current_selected_index = 0;
        showExtraCategory = DisplayCategory.NONE;
    }

    public JSViewerActivity(FSIViewerModel model, BookViewerPlace place, ClientFactory clientFactory) {
        this(place, clientFactory);
        this.model = model;
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
        LoadingPanel.INSTANCE.show();
        this.eventBus.fireEvent(new BookSelectEvent(true, book));
        panel.setWidget(view);

        archiveService.loadFSIViewerModel(collection, book, LocaleInfo.getCurrentLocale().getLocaleName(),
                new AsyncCallback<FSIViewerModel>() {
            @Override
            public void onFailure(Throwable caught) {
                logger.log(Level.SEVERE, "Failed to load image list.", caught);
                LoadingPanel.INSTANCE.hide();
            }

            @Override
            public void onSuccess(FSIViewerModel result) {
                model = result;
                images = result.getImages();

                if (starterPage != null && !starterPage.isEmpty()) {
                    current_selected_index = getImageIndex(starterPage, images);
                    if (starterPage.endsWith("v") || starterPage.endsWith("V")) {
                        current_selected_index++;
                    }
                }

                createJSviewer();

                view.setPermissionStatement(result.getPermission().getPermission());
                LoadingPanel.INSTANCE.hide();
            }
        });
    }

    public String getCurrentPage() {
        if (images == null || images.getImages() == null || images.getImages().size() < current_selected_index) {
            return null;
        }

        return images.getImages().get(current_selected_index).getId();
    }

    private void createJSviewer() {
        final String fsi_missing_image = fsi_share + "/missing_image.tif";

        archiveService.loadImageListAsString(collection, book, new AsyncCallback<String>() {
            @Override
            public void onFailure(Throwable caught) {
                logger.log(Level.SEVERE, "Failed to load image list for book. [" + collection + ":" + book + "]",
                        caught);
            }

            @Override
            public void onSuccess(String result) {
                RoseBook roseBook = new RoseBook(fsi_share, result, fsi_missing_image);
                setupView(roseBook.model());
            }
        });
    }

    private void setupView(final CodexModel codexModel) {
        final CodexController controller = new SimpleCodexController(codexModel);
        ImageServer server = new FSIImageServer(WebsiteConfig.INSTANCE.fsiUrl());

        view.setCodexView(server, codexModel, controller, viewerMode);

        view.addFirstClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                controller.gotoOpening(codexModel.opening(0));
            }
        });

        view.addLastClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                controller.gotoOpening(codexModel.opening(codexModel.numOpenings() - 1));
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

        view.addGoToKeyDownHandler(new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent event) {
                if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                    int index = getImageIndex(view.getGotoText(), images);

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

                        if (index < codexModel.numOpenings()) {
                            controller.gotoOpening(codexModel.opening(index));
                        }
                    }
                }
            }
        });

        view.addShowExtraChangeHandler(showExtraChangeHandler);

        controller.addChangeHandler(new ChangeHandler() {
            @Override
            public void openingChanged(CodexOpening opening) {
                view.setToolbarVisible(true);

                current_selected_index = opening.position() * 2;
                view.setGotoText(opening.label());
                setupShowExtra(current_selected_index, true);
            }

            @Override
            public void viewChanged(List<CodexImage> viewList) {
                view.setToolbarVisible(false);
                if (viewList.size() > 0) {
                    CodexImage img = viewList.get(0);
                    current_selected_index = codexModel.findOpeningImage(img.id());
                    setupShowExtra(current_selected_index, false);
                }
            }
        });

        switch (viewerMode) {
            case PAGE_TURNER:
                int index = current_selected_index / 2;

                if (index < codexModel.numOpenings()) {
                    controller.gotoOpening(codexModel.opening(index));
                } else {
                    controller.gotoOpening(codexModel.opening(0));
                }

                setupShowExtra(current_selected_index, true);

                break;
            case IMAGE_BROWSER:
                break;
            case IMAGE_VIEWER:
                if (current_selected_index < codexModel.numImages()) {
                    controller.setView(codexModel.image(current_selected_index));
                } else {
                    controller.setView(codexModel.nonOpeningImage(current_selected_index - codexModel.numImages()));
                }

                setupShowExtra(current_selected_index, false);
                break;
            default:
                view.setToolbarVisible(false);
                break;
        }
    }

    private void setupShowExtra(int page, boolean opening) {
        List<String> page1 = new ArrayList<>(Arrays.asList(getExtraDataLabels(page)));

        if (opening) {
            String[] page2 = getExtraDataLabels(page - 1);
            for (String label : page2) {
                if (!page1.contains(label)) {
                    page1.add(label);
                }
            }
        }
        view.setShowExtraLabels(page1.toArray(new String[page1.size()]));
        handleShowExtra();
    }

    private void handleShowExtra() {
        if (showExtraCategory == null) {
            return;
        }
        //   Generate array of String labels to label each tab
        String[] selectedPages = view.getGotoText().split(",");

        boolean lecoy = true;
        switch (showExtraCategory) {
            case TRANSCRIPTION:
                view.setSelectedShowExtra(Labels.INSTANCE.transcription());
                lecoy = false;
                // Display transcriptions for all pages/columns
                // Fall through
            case LECOY:
                view.setSelectedShowExtra(Labels.INSTANCE.transcription() + "[" + Labels.INSTANCE.lecoy() + "]");
                // Display Lecoy

                //   Generate array of Strings holding XML fragments for each relevant page
                List<String> list = new ArrayList<>();
                for (String page : selectedPages) {
                    list.add(model.getTranscription(page));
                }

                //   Create display widget and add it to view
                view.showExtra(TranscriptionViewer.createTranscriptionViewer(
                        list.toArray(new String[list.size()]), selectedPages, lecoy
                ));
                break;
            case ILLUSTRATION:
                view.setSelectedShowExtra(Labels.INSTANCE.illustrationDescription());
                // Display illustration descriptions

                view.showExtra(TranscriptionViewer.createIllustrationTaggingViewer(
                        selectedPages, model.getIllustrationTagging()));
                break;
            case NARRATIVE:
                view.setSelectedShowExtra(Labels.INSTANCE.narrativeSections());
                // Display narrative sections
                view.showExtra(TranscriptionViewer.createNarrativeTaggingViewer(selectedPages,
                        model.getNarrativeTagging(), model.getNarrativeSections()));
                break;
            case NONE:
                // Fall through to default
            default:
                view.setSelectedShowExtra(Labels.INSTANCE.show());
                view.showExtra(null);
                break;
        }

//        view.onResize();
    }

    public String[] getExtraDataLabels(int page) {
        return getExtraDataLabels(getImageName(page));
    }

    private String[] getExtraDataLabels(String page) {
        if (model == null) {
            return new String[] {Labels.INSTANCE.show()};
        }

        List<String> labels = new ArrayList<>();
        labels.add(Labels.INSTANCE.show());

        if (model.hasTranscription(page)) {
            labels.add(Labels.INSTANCE.transcription());
            labels.add(Labels.INSTANCE.transcription() + "[" + Labels.INSTANCE.lecoy() + "]");
        }
        if (model.hasIllustrationTagging(page)) {
            labels.add(Labels.INSTANCE.illustrationDescription());
        }
        if (model.hasNarrativeTagging(page)) {
            labels.add(Labels.INSTANCE.narrativeSections());
        }

        return labels.toArray(new String[labels.size()]);
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

    private int getImageIndex(String name, ImageList images) {
        if (images != null && images.getImages() != null) {
            List<BookImage> list = images.getImages();
            for (int i = 0; i < list.size(); i++) {
                BookImage image = list.get(i);
                if (image.getName().equals(name) || image.getId().equals(name)) {
                    return i;
                }
            }
        }

        return -1;
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

    private void finishActivity() {
        this.eventBus.fireEvent(new BookSelectEvent(false, book));
        LoadingPanel.INSTANCE.hide();
    }
}
