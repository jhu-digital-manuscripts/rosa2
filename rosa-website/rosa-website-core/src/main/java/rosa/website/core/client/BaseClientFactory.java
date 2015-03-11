package rosa.website.core.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;
import rosa.website.core.client.view.TestView;
import rosa.website.core.client.view.impl.TestViewImpl;

import java.util.logging.Level;
import java.util.logging.Logger;

public class BaseClientFactory {
    private static final Logger logger = Logger.getLogger(BaseClientFactory.class.toString());

    private static EventBus event_bus = new SimpleEventBus();
    private static PlaceController place_controller = new PlaceController(event_bus);
    private final StaticResourceServiceAsync staticResourceService = GWT.create(StaticResourceService.class);

    private static TestView testView;

    public EventBus eventBus() {
        return event_bus;
    }

    public PlaceController placeController() {
        return place_controller;
    }

    public TestView testView() {
        if (testView == null) {
            testView = new TestViewImpl();
        }

        return testView;
    }

    public StaticResourceServiceAsync staticResourceService() {
        if (staticResourceService == null) {
            logger.log(Level.WARNING, "Static resource service not found.");
        }
        return staticResourceService;
    }
}
