package rosa.website.viewer.client.jsviewer.dynimg;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Touch;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.GestureChangeEvent;
import com.google.gwt.event.dom.client.GestureChangeHandler;
import com.google.gwt.event.dom.client.GestureEndEvent;
import com.google.gwt.event.dom.client.GestureEndHandler;
import com.google.gwt.event.dom.client.GestureStartEvent;
import com.google.gwt.event.dom.client.GestureStartHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.dom.client.MouseWheelEvent;
import com.google.gwt.event.dom.client.MouseWheelHandler;
import com.google.gwt.event.dom.client.TouchCancelEvent;
import com.google.gwt.event.dom.client.TouchCancelHandler;
import com.google.gwt.event.dom.client.TouchEndEvent;
import com.google.gwt.event.dom.client.TouchEndHandler;
import com.google.gwt.event.dom.client.TouchMoveEvent;
import com.google.gwt.event.dom.client.TouchMoveHandler;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.dom.client.TouchStartHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.Image;

// Master image is tiled at a zoom level.
// All tiles for a zoom level arranged on the canvas in correct position.
// A viewport moves over the canvas.
// Tiles are added when viewable and then kept.

// TODO stretch old tiles before replacing?

// TODO HTML 5 version

public class ImageViewport extends Composite {
    private static final double zoom_increment_guess = 0.2; // Used to guess max
                                                            // zoom level
    private final AbsolutePanel viewport;
    private final AbsolutePanel canvas;
    private final ImageServer server;

    private int viewport_width; // TODO use getOffsetWidth?
    private int viewport_height;

    private int thumb_width, thumb_height;
    private int canvas_width, canvas_height;
    private int canvas_center_x, canvas_center_y;

    private MasterImage master;

    private WebImage[][][] tile_cache; // [zoom_level - 1][row][col]

    private final Image thumb;
    private final FocusPanel thumb_sel;

    private double zoom_level_1;
    private double zoom;
    private int zoom_level;
    private int zoom_level_max;

    private boolean drag_may_start;
    private boolean dragging;
    private int drag_x, drag_y;

    public ImageViewport(ImageServer server, int width, int height) {
        this.server = server;
        this.canvas = new AbsolutePanel();
        this.viewport = new AbsolutePanel();

        viewport.setStylePrimaryName("ImageViewport");
        viewport.setSize(width + "px", height + "px");
        viewport.add(canvas);
        DOM.setStyleAttribute(viewport.getElement(), "overflow", "hidden");

        this.viewport_width = width;
        this.viewport_height = height;

        FocusPanel top = new FocusPanel(viewport);
        top.setSize(width + "px", height + "px");

        initWidget(top);

        drag_may_start = false;
        dragging = false;

        // zoom and center
        top.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                event.preventDefault();
                event.stopPropagation();

                drag_may_start = false;

                if (dragging) {
                    dragging = false;
                    return;
                }

                int x = event.getRelativeX(canvas.getElement());
                int y = event.getRelativeY(canvas.getElement());

                // Don't allow clicking outside of the canvas

                if (x < 0 || y < 0 || x > canvas.getOffsetWidth()
                        || y > canvas.getOffsetHeight()) {
                    return;
                }

                canvas_center_x = x;
                canvas_center_y = y;

                if (zoom_level == zoom_level_max) {
                    viewportMoved();
                } else {
                    zoomLevel(zoom_level + 1);
                }
            }
        });

        // pan when mouse down
        top.addMouseDownHandler(new MouseDownHandler() {
            public void onMouseDown(MouseDownEvent event) {
                event.preventDefault();
                event.stopPropagation();

                dragging = false;

                // Don't start dragging outside the canvas

                int x = event.getRelativeX(canvas.getElement());
                int y = event.getRelativeY(canvas.getElement());

                if (x < 0 || y < 0 || x > canvas.getOffsetWidth()
                        || y > canvas.getOffsetHeight()) {
                    return;
                }

                if (event.getNativeButton() == NativeEvent.BUTTON_LEFT) {
                    drag_may_start = true;
                    drag_x = event.getClientX();
                    drag_y = event.getClientY();
                } else {
                    drag_may_start = false;
                }
            }
        });

        top.addMouseOutHandler(new MouseOutHandler() {
            public void onMouseOut(MouseOutEvent event) {
                drag_may_start = false;
                dragging = false;
            }
        });

        top.addMouseMoveHandler(new MouseMoveHandler() {
            public void onMouseMove(MouseMoveEvent event) {
                event.preventDefault();
                event.stopPropagation();

                if (drag_may_start) {
                    dragging = true;
                }

                if (dragging) {
                    int dx = drag_x - event.getClientX();
                    int dy = drag_y - event.getClientY();

                    drag_x = event.getClientX();
                    drag_y = event.getClientY();

                    pan(dx, dy);
                }
            }
        });

        top.addMouseWheelHandler(new MouseWheelHandler() {
            public void onMouseWheel(MouseWheelEvent event) {
                event.preventDefault();
                event.stopPropagation();

                drag_may_start = false;
                dragging = false;

                int v = event.getNativeEvent().getMouseWheelVelocityY();

                if (v < 0) {
                    zoomLevel(zoom_level + 1);
                } else {
                    zoomLevel(zoom_level - 1);
                }
            }
        });

        // TODO double tap to zoom out, use timeouts

        top.addTouchStartHandler(new TouchStartHandler() {
            public void onTouchStart(TouchStartEvent event) {
                event.preventDefault();
                event.stopPropagation();

                if (event.getTouches().length() != 1) {
                    return;
                }

                Touch touch = event.getTouches().get(0);

                // Don't start dragging outside the canvas

                int x = touch.getRelativeX(canvas.getElement());
                int y = touch.getRelativeY(canvas.getElement());

                if (x < 0 || y < 0 || x > canvas.getOffsetWidth()
                        || y > canvas.getOffsetHeight()) {
                    return;
                }

                dragging = false;
                drag_may_start = true;

                drag_x = touch.getClientX();
                drag_y = touch.getClientY();
            }
        });

        top.addTouchMoveHandler(new TouchMoveHandler() {
            public void onTouchMove(TouchMoveEvent event) {
                event.preventDefault();
                event.stopPropagation();

                if (event.getTouches().length() != 1) {
                    drag_may_start = false;
                    dragging = false;
                    return;
                }

                Touch touch = event.getTouches().get(0);

                if (drag_may_start) {
                    dragging = true;
                }

                if (dragging) {
                    int dx = drag_x - touch.getClientX();
                    int dy = drag_y - touch.getClientY();

                    drag_x = touch.getClientX();
                    drag_y = touch.getClientY();

                    pan(dx, dy);
                }
            }
        });

        top.addTouchEndHandler(new TouchEndHandler() {
            public void onTouchEnd(TouchEndEvent event) {
                event.preventDefault();
                event.stopPropagation();

                if (event.getChangedTouches().length() == 1 && !dragging) {
                    // click

                    Touch touch = event.getChangedTouches().get(0);
                    // Don't allow click outside the canvas

                    int x = touch.getRelativeX(canvas.getElement());
                    int y = touch.getRelativeY(canvas.getElement());

                    if (x < 0 || y < 0 || x > canvas.getOffsetWidth()
                            || y > canvas.getOffsetHeight()) {
                        return;
                    }

                    canvas_center_x = x;
                    canvas_center_y = y;

                    if (zoom_level == zoom_level_max) {
                        viewportMoved();
                    } else {
                        zoomLevel(zoom_level + 1);
                    }
                }

                drag_may_start = false;
                dragging = false;
            }
        });

        top.addGestureStartHandler(new GestureStartHandler() {
            public void onGestureStart(GestureStartEvent event) {
                event.preventDefault();
                event.stopPropagation();

                drag_may_start = false;
                dragging = false;
            }
        });

        top.addGestureChangeHandler(new GestureChangeHandler() {
            public void onGestureChange(GestureChangeEvent event) {
                event.preventDefault();
                event.stopPropagation();

                drag_may_start = false;
                dragging = false;

                double scale = event.getScale();

                if (scale > 1.0) {
                    zoomLevel(zoom_level + 1);
                } else {
                    zoomLevel(zoom_level - 1);
                }
            }
        });

        top.addGestureEndHandler(new GestureEndHandler() {
            public void onGestureEnd(GestureEndEvent event) {
                event.preventDefault();
                event.stopPropagation();

                drag_may_start = false;
                dragging = false;
            }
        });

        top.addTouchCancelHandler(new TouchCancelHandler() {
            public void onTouchCancel(TouchCancelEvent event) {
                drag_may_start = false;
                dragging = false;
            }
        });

        this.thumb = new Image();
        thumb.setStylePrimaryName("ImageViewportThumb");

        viewport.add(thumb);

        // center
        thumb.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                event.preventDefault();
                event.stopPropagation();

                canvas_center_x = scale(event.getRelativeX(thumb.getElement()),
                        thumb_width, canvas_width);
                canvas_center_y = scale(event.getRelativeY(thumb.getElement()),
                        thumb_height, canvas_height);

                viewportMoved();
            }
        });

        thumb.addMouseDownHandler(new MouseDownHandler() {
            public void onMouseDown(MouseDownEvent event) {
                event.preventDefault();
                event.stopPropagation();
            }
        });

        thumb.addTouchStartHandler(new TouchStartHandler() {
            public void onTouchStart(TouchStartEvent event) {
                event.preventDefault();
                event.stopPropagation();
                
                drag_may_start = false;
                dragging = false;
            }
        });

        thumb.addTouchMoveHandler(new TouchMoveHandler() {
            public void onTouchMove(TouchMoveEvent event) {
                event.preventDefault();
                event.stopPropagation();

                drag_may_start = false;
                dragging = false;
            }
        });

        thumb.addTouchEndHandler(new TouchEndHandler() {
            public void onTouchEnd(TouchEndEvent event) {
                event.preventDefault();
                event.stopPropagation();

                if (event.getChangedTouches().length() == 1 && !dragging) {
                    Touch touch = event.getChangedTouches().get(0);

                    canvas_center_x = scale(
                            touch.getRelativeX(thumb.getElement()),
                            thumb_width, canvas_width);
                    canvas_center_y = scale(
                            touch.getRelativeY(thumb.getElement()),
                            thumb_height, canvas_height);

                    viewportMoved();
                }

                drag_may_start = false;
                dragging = false;
            }
        });

        this.thumb_sel = new FocusPanel();
        thumb_sel.setStylePrimaryName("ImageViewportThumbSelection");

        viewport.add(thumb_sel);

        thumb_sel.addMouseMoveHandler(new MouseMoveHandler() {
            public void onMouseMove(MouseMoveEvent event) {
                event.preventDefault();
                event.stopPropagation();

                if (drag_may_start) {
                    dragging = true;
                }

                if (dragging) {
                    int dx = event.getClientX() - drag_x;
                    int dy = event.getClientY() - drag_y;

                    drag_x = event.getClientX();
                    drag_y = event.getClientY();

                    dx = scale(dx, thumb_width, canvas_width);
                    dy = scale(dy, thumb_height, canvas_height);

                    pan(dx, dy);
                }
            }
        });

        thumb_sel.addMouseDownHandler(new MouseDownHandler() {
            public void onMouseDown(MouseDownEvent event) {
                event.preventDefault();
                event.stopPropagation();

                dragging = false;

                if (event.getNativeButton() == NativeEvent.BUTTON_LEFT) {
                    drag_may_start = true;
                    drag_x = event.getClientX();
                    drag_y = event.getClientY();
                } else {
                    drag_may_start = false;
                }
            }
        });

        thumb_sel.addMouseUpHandler(new MouseUpHandler() {
            public void onMouseUp(MouseUpEvent event) {
                drag_may_start = false;
                dragging = false;
            }
        });

        thumb_sel.addMouseOutHandler(new MouseOutHandler() {
            public void onMouseOut(MouseOutEvent event) {
                drag_may_start = false;
                dragging = false;
            }
        });

        thumb_sel.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                event.preventDefault();
                event.stopPropagation();
            }
        });
        
        thumb_sel.addTouchStartHandler(new TouchStartHandler() {
            public void onTouchStart(TouchStartEvent event) {
                event.preventDefault();
                event.stopPropagation();


                if (event.getTouches().length() != 1) {
                    drag_may_start = false;
                    dragging = false;
                    return;
                }

                Touch touch = event.getTouches().get(0);
                
                dragging = false;
                drag_may_start = true;
                drag_x = touch.getClientX();
                drag_y = touch.getClientY();
            }
        });
        
        thumb_sel.addTouchMoveHandler(new TouchMoveHandler() {
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

                Touch touch = event.getTouches().get(0);
                
                if (dragging) {
                    int dx = touch.getClientX() - drag_x;
                    int dy = touch.getClientY() - drag_y;

                    drag_x = touch.getClientX();
                    drag_y = touch.getClientY();

                    dx = scale(dx, thumb_width, canvas_width);
                    dy = scale(dy, thumb_height, canvas_height);

                    pan(dx, dy);
                }
            }
        });
        
        thumb_sel.addTouchEndHandler(new TouchEndHandler() {
            public void onTouchEnd(TouchEndEvent event) {
                event.preventDefault();
                event.stopPropagation();
                
                drag_may_start = false;
                dragging = false;
            }
        });
    }

    public void display(MasterImage master) {
        display(master, -1, -1);
    }

    // TODO fix, abstract out zoom calcs

    public void display(final MasterImage master, int thumb_size,
            int max_zoom_level) {
        this.master = master;
        this.drag_may_start = false;
        this.dragging = false;

        // TODO
        if (thumb_size > 0) {
            this.thumb_width = thumb_size;
        } else {
            this.thumb_width = 128;
        }

        // zoom level 1 displays the whole image

        zoom_level = 1;
        zoom_level_1 = (double) viewport_height / master.height();

        if (zoom_level_1 * master.width() > viewport_width) {
            zoom_level_1 = (double) viewport_width / master.width();
        }

        if (zoom_level_1 > 1.0) {
            zoom_level_1 = 1.0;
            zoom_level_max = 1;
        } else if (max_zoom_level < 0) {
            zoom_level_max = (int) ((1.0 - zoom_level_1) / zoom_increment_guess);
        } else {
            this.zoom_level_max = max_zoom_level;
        }

        if (master.width() > master.height()) {
            this.thumb_height = (thumb_width * master.height())
                    / master.width();
        } else {
            this.thumb_height = thumb_width;
            this.thumb_width = (thumb_height * master.width())
                    / master.height();
        }

        viewport.setWidgetPosition(thumb, viewport_width - thumb_width,
                viewport_height - thumb_height);
        thumb.setUrl(server.render(master, thumb_width, thumb_height).url());

        canvas_width = (int) (zoom_level_1 * master.width());
        canvas_height = (int) (zoom_level_1 * master.height());

        canvas_center_x = canvas_width / 2;
        canvas_center_y = canvas_height / 2;

        this.tile_cache = new WebImage[zoom_level_max][][];

        zoomChanged();
    }

    private static final int scale(int value, int max_value, int new_max_value) {
        return (value * new_max_value) / max_value;
    }

    private void zoomChanged() {
        zoom = calculateZooom();

        // Window.alert("zoom " + zoom_level + " " + zoom);

        int new_canvas_width = (int) (zoom * master.width());
        int new_canvas_height = (int) (zoom * master.height());

        canvas_center_x = scale(canvas_center_x, canvas_width, new_canvas_width);
        canvas_center_y = scale(canvas_center_y, canvas_height,
                new_canvas_height);

        canvas_width = new_canvas_width;
        canvas_height = new_canvas_height;

        canvas.clear();
        canvas.setSize(canvas_width + "px", canvas_height + "px");

        WebImage[][] tiles = tile_cache[zoom_level - 1];

        if (tiles == null) {
            tiles = server.tile(master, canvas_width, canvas_height);
            tile_cache[zoom_level - 1] = tiles;
        }

        for (int row = 0, top = 0; row < tiles.length; top += tiles[row++][0]
                .height()) {
            for (int col = 0, left = 0; col < tiles[row].length; left += tiles[row][col++]
                    .width()) {
                WebImage tile = tiles[row][col];

                canvas.add(tile);
                canvas.setWidgetPosition(tile, left, top);
            }
        }

        viewportMoved();
    }

    private void displayVisibleTiles() {
        int num_tiles = canvas.getWidgetCount();

        int viewport_left = viewport.getAbsoluteLeft();
        int viewport_right = viewport_left + viewport_width;
        int viewport_top = viewport.getAbsoluteTop();
        int viewport_bottom = viewport_top + viewport_height;

        for (int i = 0; i < num_tiles; i++) {
            WebImage tile = (WebImage) canvas.getWidget(i);

            int left = tile.getAbsoluteLeft();
            int right = left + tile.width();
            int top = tile.getAbsoluteTop();
            int bottom = top + tile.height();

            if (right < viewport_left || left > viewport_right
                    || bottom < viewport_top || top > viewport_bottom) {
                continue;
            }

            tile.makeViewable();
        }
    }

    /**
     * Reset the display of the current image.
     */
    public void resetDisplay() {
        if (master != null) {
            zoomLevel(1);
            center(master.width() / 2, master.height() / 2);
        }
    }
    
    public void pan(int canvas_dx, int canvas_dy) {
        canvas_center_x += canvas_dx;
        canvas_center_y += canvas_dy;

        if (master != null) {
            viewportMoved();
        }
    }

    public void center(int master_x, int master_y) {
        canvas_center_x = scale(master_x, master.width(), canvas_width);
        canvas_center_y = scale(master_y, master.height(), canvas_height);

        if (master != null) {
            viewportMoved();
        }
    }

    private void viewportMoved() {
        // Find location of viewport within the canvas
        int viewport_left = canvas_center_x - (viewport_width / 2);
        int viewport_top = canvas_center_y - (viewport_height / 2);

        // Window.alert("viewport left,top " + viewport_left + "," +
        // viewport_top);

        // Move viewport to correct location by shifting canvas
        viewport.setWidgetPosition(canvas, -viewport_left, -viewport_top);

        displayVisibleTiles();

        // put current viewport in thumb

        int sel_left = scale(viewport_left, canvas_width, thumb_width);
        int sel_right = scale(viewport_left + viewport_width, canvas_width,
                thumb_width);
        int sel_top = scale(viewport_top, canvas_height, thumb_height);
        int sel_bottom = scale(viewport_top + viewport_height, canvas_height,
                thumb_height);

        if (sel_left < 0) {
            sel_left = 0;
        }

        if (sel_right > thumb_width) {
            sel_right = thumb_width;
        }

        if (sel_top < 0) {
            sel_top = 0;
        }

        if (sel_bottom > thumb_height) {
            sel_bottom = thumb_height;
        }

        thumb_sel.setSize((sel_right - sel_left) + "px", (sel_bottom - sel_top)
                + "px");

        viewport.setWidgetPosition(thumb_sel, viewport_width - thumb_width
                + sel_left, viewport_height - thumb_height + sel_top);
    }

    /**
     * Zoom level 1 displays the whole image.
     * 
     * @param level
     */
    public void zoomLevel(int level) {
        int newzoom = level < 1 ? 1 : (level > zoom_level_max ? zoom_level_max
                : level);

        if (zoom_level != newzoom) {
            zoom_level = newzoom;
            zoomChanged();
        }
    }

    public int zoomLevel() {
        return zoom_level;
    }

    private double calculateZooom() {
        if (zoom_level == 1) {
            return zoom_level_1;
        } else if (zoom_level == zoom_level_max) {
            return 1.0;
        } else {
            // TODO linear scaling not user friendly?
            return ((1.0 - zoom_level_1) * zoom_level) / zoom_level_max;
        }
    }

    // // TODO support resize
    // public void onResize() {
    // // TODO does getOffset work?
    // viewport_width = getOffsetWidth();
    // viewport_height = getOffsetHeight();
    //
    // if (master != null) {
    // viewportMoved();
    // }
    // }
}
