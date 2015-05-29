package rosa.website.rose.client.activity;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import rosa.website.core.client.ClientFactory;
import rosa.website.core.client.event.FlashStatusChangeEvent;
import rosa.website.core.client.event.FlashStatusChangeEventHandler;
import rosa.website.core.client.place.BookViewerPlace;

import java.util.ArrayList;
import java.util.List;

/**
 * Starts/stops different activities for flash/JS based viewers.
 */
public class BookViewerActivity implements Activity, FlashStatusChangeEventHandler {

    private FSIViewerActivity fsiActivity;
    private JSViewerActivity jsActivity;

    private final ClientFactory clientFactory;
    private final BookViewerPlace initialPlace;
    private boolean useFlash;

    private List<HandlerRegistration> handlers;
    private AcceptsOneWidget container;
    private EventBus eventBus;

    /**
     * Create a new BookViewerActivity.
     *
     * @param place initial state
     * @param clientFactory .
     */
    public BookViewerActivity(BookViewerPlace place, ClientFactory clientFactory) {
        this.fsiActivity = new FSIViewerActivity(place, clientFactory);
        this.jsActivity = new JSViewerActivity(place, clientFactory);
        this.clientFactory = clientFactory;
        this.initialPlace = place;

        this.useFlash = clientFactory.context().useFlash();
        this.handlers = new ArrayList<>();
    }

    @Override
    public String mayStop() {
        return null;
    }

    @Override
    public void onCancel() {
        clearHandlers();
        if (fsiActivity != null) {
            fsiActivity.onCancel();
        }
        if (jsActivity != null) {
            jsActivity.onCancel();
        }
    }

    @Override
    public void onStop() {
        clearHandlers();
        if (fsiActivity != null) {
            fsiActivity.onStop();
        }
        if (jsActivity != null) {
            jsActivity.onStop();
        }
    }

    @Override
    public void start(AcceptsOneWidget panel, EventBus eventBus) {
        this.container = panel;
        this.eventBus = eventBus;
        handlers.add(eventBus.addHandler(FlashStatusChangeEvent.TYPE, this));

        if (useFlash) {
            fsiActivity.start(panel, eventBus);
        } else {
            jsActivity.start(panel, eventBus);
        }
    }

    @Override
    public void onFlashStatusChange(FlashStatusChangeEvent event) {
        if (event.status() == useFlash) {
            return;
        }

        useFlash = event.status();
        if (useFlash) {
            fsiActivity = new FSIViewerActivity(
                    new BookViewerPlace(initialPlace.getType(), initialPlace.getBook(), jsActivity.getCurrentPage()),
                    clientFactory
            );

            jsActivity.onStop();
            jsActivity = null;

            fsiActivity.start(container, eventBus);
        } else {
            jsActivity = new JSViewerActivity(
                    new BookViewerPlace(initialPlace.getType(), initialPlace.getBook(), fsiActivity.getCurrentPage()),
                    clientFactory
            );

            fsiActivity.onStop();
            fsiActivity = null;

            jsActivity.start(container, eventBus);
        }
    }

    private void clearHandlers() {
        for (HandlerRegistration h : handlers) {
            if (h != null) {
                h.removeHandler();
            }
        }
        handlers.clear();
    }
}
