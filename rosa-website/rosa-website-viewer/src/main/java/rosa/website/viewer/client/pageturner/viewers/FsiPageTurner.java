package rosa.website.viewer.client.pageturner.viewers;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.TouchEndEvent;
import com.google.gwt.event.dom.client.TouchEndHandler;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;

import rosa.website.viewer.client.pageturner.model.Book;
import rosa.website.viewer.client.pageturner.model.Opening;
import rosa.website.viewer.client.pageturner.model.Page;
import rosa.website.viewer.client.pageturner.util.Console;
import rosa.website.viewer.client.pageturner.util.FadeAnimation;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class FsiPageTurner extends Composite implements PageTurner, HasClickHandlers, HasValueChangeHandlers<Opening> {
    private static final Map<String, String> SHARED_VIEWER_OPTIONS = new HashMap<>();
    private static final Map<String, String> IMAGE_FLOW_OPTIONS = new HashMap<>();
    private static final int FADE_LENGTH = 125;     // milliseconds

    static {
        SHARED_VIEWER_OPTIONS.put("enableZoom", "false");
        SHARED_VIEWER_OPTIONS.put("hideUI", "true");
        SHARED_VIEWER_OPTIONS.put("plugins", "FullScreen, Resize");

        IMAGE_FLOW_OPTIONS.put("mirrorHeight", "0");
        IMAGE_FLOW_OPTIONS.put("curveHeight", "0.5");
        IMAGE_FLOW_OPTIONS.put("elementSpacing", "10");
        IMAGE_FLOW_OPTIONS.put("enableZoom", "false");
        IMAGE_FLOW_OPTIONS.put("paddingTop", "0");
        IMAGE_FLOW_OPTIONS.put("callBackStart", "imageFlowStart");
        IMAGE_FLOW_OPTIONS.put("callBackClick", "imageFlowClick");
    }

    /**
     * When left or right pages are clicked, this view takes over the PageTurner and
     * gives the user full zoom/pan controls for the selected page.
     */
    private final FsiViewer zoomView;
    /** Static viewer for showing left page (verso) */
    private final FsiViewer left;
    /** Static viewer for showing right page (recto) */
    private final FsiViewer right;
    /** FSI Image Flow for showing thumbnails */
    private final FsiImageFlow thumbnailStrip;
    /** Container for any custom controls for this PageTurner. EX: next/prev buttons */
    private final Panel controls;

    /** Close button for minimizing the <em>zoomView</em> */
    private Anchor closeBtn;
    /** Label for displaying currently visible page labels */
    private Label openingLabel;

    /** Print debug statements in the browser JavaScript console */
    private boolean debug;
    /** Is the <em>zoomView</em> currently active? */
    private boolean zoomed;

    /** PageTurner data model. Includes a list of openings. */
    private final Book model;

    private int currentOpening;
    private String preferredWidth;
    private String preferredHeight;

    private final FadeAnimation zoomViewFadeAnimation;
    private FadeAnimation closeBtnFadeAnimation;

    /**
     * Create a new FSI Page Turner widget with debugging on.
     *
     * @param book data model
     * @param thumbSrcs list of images to define thumbnail order
     * @param debug turn debug on?
     */
    public FsiPageTurner(Book book, String[] thumbSrcs, boolean debug) {
        this(book, thumbSrcs);
        this.debug = debug;

        left.setDebug(debug);
        right.setDebug(debug);
        zoomView.setDebug(debug);
        thumbnailStrip.setDebug(debug);

        debug("Initializing page turner with model: " + book.toString());
    }

    public FsiPageTurner(Book book, String[] thumbSrcs) {
        this.model = book;
        this.currentOpening = 0;

        Panel root = new FlowPanel("fsi-rosa-pageturner");
        root.setSize("100%", "100%");

        controls = new FlowPanel("div");
        thumbnailStrip = new FsiImageFlow();

        controls.setStyleName("pageturner-controls");
        thumbnailStrip.setStyleName("pageturner-thumbs");

        String fsiDir = book.fsiDirectory;
        fsiDir = fsiDir.endsWith("/") ? fsiDir : fsiDir + "/";

        Opening start = book.getOpening(0);

// ---------------------------------------------------------------------------------------------------------------------
// ---------- Left viewer ----------------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------
        left = createViewer(start.verso, "rosa-pageturner-left", "pageturner-left", SHARED_VIEWER_OPTIONS);

// ---------------------------------------------------------------------------------------------------------------------
// ----------- Right viewer --------------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------
        right = createViewer(start.recto, "rosa-pageturner-right", "pageturner-right", SHARED_VIEWER_OPTIONS);

// ---------------------------------------------------------------------------------------------------------------------
// ----------- Thumbnails ----------------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------
        Map<String, String> options = new HashMap<>(IMAGE_FLOW_OPTIONS);
        if (thumbSrcs == null || thumbSrcs.length == 0) {
            options.put("dir", fsiDir);
        } else {
            StringBuilder list = new StringBuilder();
            for (int i = 0; i < thumbSrcs.length; i++) {
                String src = thumbSrcs[i];
                if (src == null || src.isEmpty()) {
                    continue;
                }

                if (i > 0) {
                    list.append(",");
                }
                list.append(src);
            }
            options.put("imagesources", list.toString());
        }

        thumbnailStrip.setId("rosa-thumbnails");
        thumbnailStrip.setOptions(options);

// ---------------------------------------------------------------------------------------------------------------------
// ----------- Zoom viewer ---------------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------
        zoomView = new FsiViewer();
        zoomView.setStyleName("pageturner-zoomview");
        options = new HashMap<>();
        options.put("plugins", "FullScreen, Resize");
        options.put("src", book.missingImage.id);
        options.put("inPlaceZoom", "true");
        zoomView.setId("rosa-pageturner-zoomview");
        zoomView.setOptions(options);

        root.add(left);
        root.add(right);
        root.add(controls);
        root.add(thumbnailStrip);
        root.add(zoomView);

        initWidget(root);

        createViewerCallbacks(this);
        setControls();

        Window.addResizeHandler(new ResizeHandler() {
            @Override
            public void onResize(ResizeEvent event) {
                resize();
            }
        });

        // Defer: initialize FSI instances, set appropriate pages, set correct size.
        Scheduler.get().scheduleDeferred(new ScheduledCommand() {
            @Override
            public void execute() {
                fsiInit();
                setOpening(0);
                resize();
            }
        });

        this.zoomViewFadeAnimation = new FadeAnimation(zoomView.getElement());
        if (closeBtn != null) {
            this.closeBtnFadeAnimation = new FadeAnimation(closeBtn.getElement());
        }
    }

    @Override
    public void setSize(String width, String height) {
        super.setSize(width, height);

        int w = Integer.parseInt(width.substring(0, width.length() - 2));
        int h = Integer.parseInt(height.substring(0, height.length() - 2)) -
                thumbnailStrip.getOffsetHeight() - controls.getOffsetHeight();

        if (left != null) {
            left.setSize((w / 2) - 1 + "px", h + "px");
        }
        if (right != null) {
            right.setSize((w / 2) - 1 + "px", h + "px");
        }
        if (zoomView != null) {
            zoomView.setSize(width, h + "px");
        }

        this.preferredWidth = width;
        this.preferredHeight = height;
    }

    /**
     * @return current opening visible in viewer, NULL if none was found
     */
    @Override
    public Opening currentOpening() {
        return model.getOpening(currentOpening);
    }

    @Override
    public HandlerRegistration addOpeningChangedHandler(ValueChangeHandler<Opening> handler) {
        debug("New handler added to deal with changing openings.");
        return addHandler(handler, ValueChangeEvent.getType());
    }

    @Override
    public void setOpening(int index) {
        changeToOpening(model.getOpening(index));
    }

    @Override
    public void setOpening(Opening opening) {
        changeToOpening(opening);
    }

    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Opening> handler) {
        return addOpeningChangedHandler(handler);
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
        this.left.setDebug(debug);
        this.right.setDebug(debug);
        this.zoomView.setDebug(debug);
        this.thumbnailStrip.setDebug(debug);
    }

    @Override
    public HandlerRegistration addClickHandler(ClickHandler handler) {
        return addDomHandler(handler, ClickEvent.getType());
    }

    private FsiViewer createViewer(Page targetPage, String id, String cssClass, Map<String, String> defaultOptions) {
        final FsiViewer viewer = new FsiViewer();

        Map<String, String> options = new HashMap<>(defaultOptions);
        if (targetPage != null) {
            options.put("src", targetPage.id);
        }

        viewer.setStyleName("viewer");
        viewer.addStyleName(cssClass);
        viewer.setId(id);
        viewer.setOptions(options);

        viewer.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (!zoomed) {
                    zoomOnPage(viewer);
                }
            }
        });
        viewer.addTouchEndHandler(new TouchEndHandler() {
            @Override
            public void onTouchEnd(TouchEndEvent event) {
                if (!zoomed) {
                    zoomOnPage(viewer);
                }
            }
        });

        return viewer;
    }

    private void zoomOnPage(FsiViewer viewer) {
        zoomed = true;
        closeBtn.setVisible(true);
        zoomView.getElement().getStyle().setZIndex(500);
        String clickedImage = viewer.getElement().getAttribute("src");
        zoomView.changeImage(clickedImage);
        closeBtnFadeAnimation.fadeIn(FADE_LENGTH);
        zoomViewFadeAnimation.fadeIn(FADE_LENGTH);
    }

    private void setControls() {
        openingLabel = new Label();
        openingLabel.setStyleName("pageturner-label");

        controls.add(openingLabel);

        closeBtn = newControl(new String[]{"fa", "fa-2x",  "fa-times"}, "Close zoom view", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (zoomed) {
                    closeBtnFadeAnimation.fadeOut(FADE_LENGTH);
                    zoomViewFadeAnimation.fadeOut(FADE_LENGTH);

                    new Timer() {
                        private int count = 0;
                        @Override
                        public void run() {
                            if (count++ > 5000 || !zoomViewFadeAnimation.isRunning()) {
                                closeBtn.setVisible(false);
                                zoomView.getElement().getStyle().setZIndex(1);
                                zoomed = false;
                                cancel();
                            }
                        }
                    }.scheduleRepeating(10);
                }
            }
        });
        closeBtn.setStyleName("pageturner-btn-zoom-close");
        closeBtn.setVisible(false);
        closeBtn.getElement().getStyle().setOpacity(0.0d);
        controls.add(closeBtn);

        controls.add(newControl(new String[]{"fa", "fa-lg", "fa-step-backward"}, "First page", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                Opening first = model.getOpening(0);
                changeToOpening(first);
            }
        }));
        controls.add(newControl(new String[]{"fa", "fa-lg", "fa-chevron-left"}, "Previous page", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                int prev = currentOpening - 1;
                if (prev >= 0) {
                    Opening opening = model.getOpening(prev);
                    changeToOpening(opening);
                }
            }
        }));
        controls.add(newControl(new String[]{"fa", "fa-lg", "fa-chevron-right"}, "Next page", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                int next = currentOpening + 1;
                if (next < model.openings.size()) {
                    Opening opening = model.getOpening(next);
                    changeToOpening(opening);
                }
            }
        }));
        controls.add(newControl(new String[]{"fa", "fa-lg", "fa-step-forward"}, "Last page", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                Opening last = model.getOpening(model.openings.size() - 1);
                changeToOpening(last);
            }
        }));
    }

    /**
     * Create a new UI control for the page turner. This control is not bound to the DOM
     * during this method, so it can be inserted anywhere.
     *
     * @param iconClasses array of class names to specify the icon. EX: ["fa", "fa-lg", "fa-chevron-down"]
     * @param title icon title/tool tip text
     * @param onClick onclick callback
     * @return the control element that can be added to the UI
     */
    private Anchor newControl(String[] iconClasses, String title, ClickHandler onClick) {
        Anchor a = new Anchor();
        a.setHref("javascript:;");
        a.setTitle(title);

        if (iconClasses != null) {
            Element icon = Document.get().createElement("i");
            for (String name : iconClasses) {
                icon.addClassName(name);
            }
            a.getElement().appendChild(icon);
            a.addClickHandler(onClick);
        }

        return a;
    }

    /**
     * Handle events generated when a user clicks on a thumbnail. By default, this
     * widget will show the book opening containing the selected thumbnail. All event
     * handlers added for this event will then be run in order that they were added.
     *
     * NOTE: this method is called from native JavaScript, the 'imageFlowClick'
     * function.
     *
     * @param strImagePath ID of clicked image
     * @param nImageIndex index of clicked image in image list
     */
    private void onThumbnailClick(String strImagePath, int nImageIndex) {
        debug("[PageTurner#onThumbnailClick] " + nImageIndex + "\n" + strImagePath);
        Opening newOpening = model.getOpening(strImagePath);
        if (newOpening != null) {
            changeToOpening(newOpening);
            currentOpening = newOpening.position;
            if (zoomed) {
                zoomView.changeImage(strImagePath);
            }
        }
    }

    private void changeToOpening(Opening opening) {
        if (opening.position >= model.openings.size()) {
            return;
        }
        int increment = 0;
        currentOpening = opening.position;

        debug("[PageTurner] Changing to opening: " + opening.toString());
        if (opening.verso == null || opening.verso.missing) {
            increment--;
            left.changeImage(model.missingImage.id);
        } else {
            left.changeImage(opening.verso.id);
        }
        if (opening.recto == null || opening.recto.missing) {
            increment--;
            right.changeImage(model.missingImage.id);
        } else {
            right.changeImage(opening.recto.id);
        }

        thumbnailStrip.focusImage(opening.position == 0 ? 0 : opening.position * 2 + increment);

        // Also change label describing visible pages
        openingLabel.setText(
                (pageHasLabel(opening.verso) ? opening.verso.label : "") +
                (pageHasLabel(opening.verso) && pageHasLabel(opening.recto) ? ", " : "") +
                (pageHasLabel(opening.recto) ? opening.recto.label : "")
        );

        if (zoomed && opening.recto != null) {
            zoomView.changeImage(opening.recto.id);
        } else if (zoomed && opening.verso != null) {
            zoomView.changeImage(opening.verso.id);
        }

        ValueChangeEvent.fire(this, opening);
    }

    private void resize() {
        if (thumbnailStrip == null || !isEmpty(preferredWidth) || !isEmpty(preferredHeight)) {
            return;
        }

        int width = getOffsetWidth();
        int height = getOffsetHeight() - controls.getOffsetHeight() - thumbnailStrip.getOffsetHeight();

        if (left != null) {
            left.setSize(width / 2 + "px", height + "px");
        }
        if (right != null) {
            right.setSize(width / 2 + "px", height + "px");
        }
        if (zoomView != null) {
            zoomView.setSize(width + "px", height + "px");
        }
        if (openingLabel != null) {
            openingLabel.getElement().getStyle().setTop(height + 6, Style.Unit.PX);
        }
    }

    private native void createViewerCallbacks(FsiPageTurner el) /*-{
        $wnd.imageFlowClick = function(oInstance, idElement, nImageIndex, strImagePath) {
            el.@rosa.website.viewer.client.pageturner.viewers.FsiPageTurner::onThumbnailClick(Ljava/lang/String;I)(strImagePath, nImageIndex);
        }
    }-*/;

    private native void fsiInit() /*-{
        console.log("Initializing FSI tags.");
        $wnd.$FSI.initCustomTags();
    }-*/;

    private void debug(String message) {
        if (debug) {
            Console.log(message);
        }
    }

    private boolean pageHasLabel(Page p) {
        return p != null && !isEmpty(p.label);
    }

    private boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }
}
