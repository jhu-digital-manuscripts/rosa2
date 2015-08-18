package rosa.website.viewer.client.jsviewer.codexview;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Touch;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseWheelEvent;
import com.google.gwt.event.dom.client.MouseWheelHandler;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.dom.client.TouchCancelEvent;
import com.google.gwt.event.dom.client.TouchCancelHandler;
import com.google.gwt.event.dom.client.TouchEndEvent;
import com.google.gwt.event.dom.client.TouchEndHandler;
import com.google.gwt.event.dom.client.TouchMoveEvent;
import com.google.gwt.event.dom.client.TouchMoveHandler;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.dom.client.TouchStartHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.StackLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import rosa.website.viewer.client.jsviewer.codexview.CodexController.ChangeHandler;
import rosa.website.viewer.client.jsviewer.dynimg.ImageServer;
import rosa.website.viewer.client.jsviewer.dynimg.ImageViewport;
import rosa.website.viewer.client.jsviewer.dynimg.WebImage;

import java.util.List;

// TODO crop switching

public class CodexView extends Composite {
    private static final int MIN_SWIPE_X = 30;
    private static final int MAX_SWIPE_Y = 20;

    private final FlowPanel main;

    private final CodexModel model;
    private final CodexController ctrl;
    private final ImageServer server;

    // Ancestor in the DOM used for efficient thumbnail loading by image browser
    private final ScrollPanel container;

    // Max space that should be used when displaying an image
    private int image_viewport_width, image_viewport_height;

    private int thumb_size;

    private Mode mode;

    private boolean drag_may_start;
    private boolean dragging;
    private int drag_x, drag_y;

    public enum Mode {
        IMAGE_BROWSER, PAGE_TURNER, IMAGE_VIEWER
    }

    public CodexView(ImageServer server, final CodexModel model, final CodexController ctrl, ScrollPanel container) {
        this.main = new FlowPanel();
        this.model = model;
        this.ctrl = ctrl;
        this.thumb_size = 128;
        this.server = server;
        this.container = container;

        main.setStylePrimaryName("CodexView");

        ctrl.addChangeHandler(new ChangeHandler() {
            public void viewChanged(List<CodexImage> view) {
                setMode(Mode.IMAGE_VIEWER);
            }

            public void openingChanged(CodexOpening opening) {
                if (mode == Mode.PAGE_TURNER) {
                    turnPage(opening);
                } else {
                    setMode(Mode.PAGE_TURNER);
                }
            }
        });

        // Update thumbs when scrolling

        final HandlerRegistration reg1 = container.addScrollHandler(new ScrollHandler() {
            public void onScroll(ScrollEvent event) {
                displayVisibleThumbs();
            }
        });

        // Remove handlers when widget no longer attached

        main.addAttachHandler(new AttachEvent.Handler() {
            public void onAttachOrDetach(AttachEvent event) {
                if (!event.isAttached()) {
                    reg1.removeHandler();
                }
            }
        });

        initWidget(main);
    }

    public void resize(int width, int height) {
        image_viewport_width = width;
        image_viewport_height = height;

        setup();
    }

    private static class Thumb extends Composite {
        private final FlexTable thumb;
        private final WebImage left;
        private final WebImage right;

        Thumb(ImageServer server, CodexImage image, int thumb_size) {
            this(image == null || image.missing() ? null : server
                    .renderToSquare(image, thumb_size), null, image.label());
        }

        Thumb(ImageServer server, CodexOpening open, int thumb_size) {
            this(open.verso() == null || open.verso().missing() ? null : server
                    .renderToSquare(open.verso(), thumb_size),
                    open.recto() == null || open.recto().missing() ? null
                            : server.renderToSquare(open.recto(), thumb_size),
                    open.label());
        }

        Thumb(WebImage left, WebImage right, String label) {
            this.thumb = new FlexTable();
            this.left = left;
            this.right = right;

            thumb.setCellPadding(0);
            thumb.setCellSpacing(0);

            thumb.setStylePrimaryName("CodexBrowserView-Thumb");

            if (left != null) {
                thumb.setWidget(0, 0, left);
                thumb.getCellFormatter().setAlignment(0, 0, HasHorizontalAlignment.ALIGN_RIGHT,
                        HasVerticalAlignment.ALIGN_MIDDLE);
            }

            if (right != null) {
                thumb.setWidget(0, 1, right);
                thumb.getCellFormatter().setAlignment(0, 1, HasHorizontalAlignment.ALIGN_LEFT,
                        HasVerticalAlignment.ALIGN_MIDDLE);
            }

            if (label != null) {
                thumb.setWidget(1, 0, new Label(label));
                thumb.getFlexCellFormatter().setColSpan(1, 0, 2);
            }

            initWidget(thumb);
        }

        void addClickHandler(ClickHandler ch) {
            if (left != null) {
                left.addClickHandler(ch);
            }

            if (right != null) {
                right.addClickHandler(ch);
            }
        }

        void makeViewable() {
            if (left != null) {
                left.makeViewable();
            }

            if (right != null) {
                right.makeViewable();
            }
        }
    }

    private void displayVisibleThumbs() {
        if (mode != Mode.IMAGE_BROWSER) {
            return;
        }

        int left = container.getAbsoluteLeft();
        int right = left + container.getOffsetWidth();
        int top = container.getAbsoluteTop();
        int bottom = top + container.getOffsetHeight();

        // This widget may be occupying more space than is
        // viewable because an ancestor might be a ScrollPanel.

        // Window.alert("here, left " + left + " top " + top + " right " + right
        // + " bottom " + bottom);

        for (int i = 0, n = main.getWidgetCount(); i < n; i++) {
            Thumb thumb = (Thumb) main.getWidget(i);

            int thumb_left = thumb.getAbsoluteLeft();
            int thumb_top = thumb.getAbsoluteTop();

            // Window.alert("thumb left " + thumb_left + " top " + thumb_top);

            if (thumb_left >= left && thumb_top >= top && thumb_left < right
                    && thumb_top < bottom) {
                thumb.makeViewable();
            }
        }
    }

    public void setMode(Mode mode) {
        if (mode == this.mode) {
            return;
        }

        this.mode = mode;
        setup();
    }

    private void setup() {
        if (mode == Mode.IMAGE_BROWSER) {
            setupImageBrowser();
        } else if (mode == Mode.PAGE_TURNER) {
            setupPageTurner();
        } else if (mode == Mode.IMAGE_VIEWER) {
            setupImageViewer();
        }
    }

    private int round100(int length) {
        int n = length / 100;

        if (n < 1) {
            n = 1;
        }

        return n * 100;
    }

    private Widget createImageViewport(int width, int height, CodexImage img) {
        final ImageViewport vp = new ImageViewport(server, width, height);

        vp.display(img);

        final FlowPanel toolbar = new FlowPanel();
        toolbar.setStylePrimaryName("CodexBrowserView-Toolbar");
        
        // TODO translate, should have separate label resource..., move to icons?
        Button zoom_in = new Button("Zoom in");
        Button zoom_out = new Button("Zoom out");
        Button zoom_orig = new Button("Reset");

        toolbar.add(zoom_in);
        toolbar.add(zoom_out);
        toolbar.add(zoom_orig);

        zoom_in.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                vp.zoomLevel(vp.zoomLevel() + 1);
            }
        });

        zoom_out.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                vp.zoomLevel(vp.zoomLevel() - 1);
            }
        });

        zoom_orig.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                vp.resetDisplay();
            }
        });

        FlowPanel panel = new FlowPanel();

        panel.add(vp);
        panel.add(toolbar);

        return panel;
    }

    private void setupImageViewer() {
        int width = round100(image_viewport_width);
        int height = round100(image_viewport_height);

        // Window.alert("dim " + content.getOffsetWidth() + " "
        // + getOffsetHeight());

        main.clear();

        if (ctrl.view().size() == 1) {
            main.add(createImageViewport(width, height, ctrl.view().get(0)));
        } else if (ctrl.view().size() > 1) {
            StackLayoutPanel stack = new StackLayoutPanel(Unit.EM);

            for (CodexImage img : ctrl.view()) {
                stack.add(createImageViewport(width, height, img), img.label(),
                        1.5);
            }

            main.add(stack);
        }
    }

    private void setupPageTurner() {
        main.clear();

        final AbsolutePanel display = new AbsolutePanel();
        FocusPanel focus = new FocusPanel(display);

        main.add(focus);

        focus.addTouchStartHandler(new TouchStartHandler() {
            public void onTouchStart(TouchStartEvent event) {
                event.preventDefault();
                event.stopPropagation();

                if (event.getTouches().length() != 1) {
                    return;
                }

                Touch touch = event.getTouches().get(0);

                dragging = false;
                drag_may_start = true;

                drag_x = touch.getClientX();
                drag_y = touch.getClientY();
            }
        });

        focus.addTouchMoveHandler(new TouchMoveHandler() {
            public void onTouchMove(TouchMoveEvent event) {
                event.preventDefault();
                event.stopPropagation();

                if (event.getTouches().length() != 1) {
                    drag_may_start = false;
                    dragging = false;
                    return;
                }

                if (drag_may_start) {
                    dragging = true;
                }
            }
        });

        focus.addTouchEndHandler(new TouchEndHandler() {
            public void onTouchEnd(TouchEndEvent event) {
                event.preventDefault();

                if (dragging && event.getChangedTouches().length() == 1) {
                    Touch touch = event.getChangedTouches().get(0);

                    int dx = drag_x - touch.getClientX();
                    int dy = drag_y - touch.getClientY();

                    if (Math.abs(dy) < MAX_SWIPE_Y
                            && Math.abs(dx) > MIN_SWIPE_X) {
                        if (dx > 0) {
                            ctrl.gotoNextOpening();
                        } else {
                            ctrl.gotoPreviousOpening();
                        }
                    }
                }

                drag_may_start = false;
                dragging = false;
            }
        });

        focus.addTouchCancelHandler(new TouchCancelHandler() {
            public void onTouchCancel(TouchCancelEvent event) {
                drag_may_start = false;
                dragging = false;
            }
        });

        focus.addMouseWheelHandler(new MouseWheelHandler() {
            public void onMouseWheel(MouseWheelEvent event) {
                event.preventDefault();
                event.stopPropagation();

                drag_may_start = false;
                dragging = false;

                int v = event.getNativeEvent().getMouseWheelVelocityY();

                if (v < 0) {
                    ctrl.gotoNextOpening();
                } else {
                    ctrl.gotoPreviousOpening();
                }
            }
        });

        turnPage(ctrl.getOpening());
    }

    // TODO page turn animation, see ScrollAnimation

    private void turnPage(final CodexOpening opening) {
        FocusPanel focus = (FocusPanel) main.getWidget(0);
        AbsolutePanel display = (AbsolutePanel) focus.getWidget();

        int page_size = round100(image_viewport_width / 2);

        WebImage verso = opening.verso() == null ? null : server.renderToSquare(opening.verso(), page_size);
        WebImage recto = opening.recto() == null ? null : server.renderToSquare(opening.recto(), page_size);

        int verso_width, verso_height, recto_width, recto_height;

        if (verso == null) {
            verso_width = 0;
            verso_height = 0;
        } else {
            verso_width = verso.width();
            verso_height = verso.height();
        }

        if (recto == null) {
            recto_width = 0;
            recto_height = 0;
        } else {
            recto_width = recto.width();
            recto_height = recto.height();
        }

        int width = verso_width + recto_width;
        int height = verso_height == 0 ? recto_height : verso_height;

        display.setSize(width + "px", height + "px");
        display.clear();

        if (verso != null) {
            verso.makeViewable();

            verso.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent event) {
                    if (!opening.verso().missing()) {
                        ctrl.setView(opening.verso());
                    }
                }
            });

            verso.addTouchEndHandler(new TouchEndHandler() {
                public void onTouchEnd(TouchEndEvent event) {
                    event.preventDefault();

                    if (!dragging && event.getChangedTouches().length() == 1) {
                        ctrl.setView(opening.verso());
                    }

                    // Allow touch end handler in parent to take care of rest
                }
            });

            display.add(verso);
            display.setWidgetPosition(verso, 0, 0);
        }

        if (recto != null) {
            recto.makeViewable();

            recto.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent event) {
                    if (!opening.recto().missing()) {
                        ctrl.setView(opening.recto());
                    }
                }
            });

            recto.addTouchEndHandler(new TouchEndHandler() {
                public void onTouchEnd(TouchEndEvent event) {
                    event.preventDefault();

                    if (!dragging && event.getChangedTouches().length() == 1) {
                        ctrl.setView(opening.recto());
                    }

                    // Allow touch end handler in parent to take care of rest
                }
            });

            display.add(recto);
            display.setWidgetPosition(recto, verso_width, 0);
        }
    }

    private void setupImageBrowser() {
        main.clear();

        for (int i = 0, n = model.numOpenings(); i < n; i++) {
            final CodexOpening opening = model.opening(i);

            Thumb thumb = new Thumb(server, opening, thumb_size);

            thumb.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent event) {
                    ctrl.gotoOpening(opening);
                }
            });

            main.add(thumb);
        }

        for (int i = 0, n = model.numNonOpeningImages(); i < n; i++) {
            final CodexImage img = model.nonOpeningImage(i);

            Thumb thumb = new Thumb(server, img, thumb_size);

            thumb.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent event) {
                    if (!img.missing()) {
                        ctrl.setView(img);
                    }
                }
            });

            main.add(thumb);
        }

        updateThumbsAfterLayout();
    }

    private void updateThumbsAfterLayout() {
        Scheduler.get().scheduleDeferred(new ScheduledCommand() {
            public void execute() {
                displayVisibleThumbs();
            }
        });
    }

    protected void onLoad() {
        super.onLoad();
        updateThumbsAfterLayout();
    }

    private native void console(String message) /*-{
        console.log(message);
    }-*/;
}
