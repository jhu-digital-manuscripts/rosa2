package rosa.website.core.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;
import rosa.website.core.client.view.CSVDataView;
import rosa.website.core.client.view.HTMLView;
import rosa.website.core.client.view.impl.CSVDataViewImpl;
import rosa.website.core.client.view.impl.HTMLViewImpl;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientFactory {
    private static final Logger logger = Logger.getLogger(ClientFactory.class.toString());

    private static EventBus event_bus = new SimpleEventBus();
    private static PlaceController place_controller = new PlaceController(event_bus);
    private final StaticResourceServiceAsync staticResourceService = GWT.create(StaticResourceService.class);

    private static HTMLView htmlView;
    private static CSVDataView csvDataView;

    public EventBus eventBus() {
        return event_bus;
    }

    public PlaceController placeController() {
        return place_controller;
    }

    public StaticResourceServiceAsync staticResourceService() {
        if (staticResourceService == null) {
            logger.log(Level.WARNING, "Static resource service not found.");
        }
        return staticResourceService;
    }

    public HTMLView htmlView() {
        if (htmlView == null) {
            htmlView = new HTMLViewImpl();
        }
        return htmlView;
    }

    public CSVDataView csvDataView() {
        if (csvDataView == null) {
            csvDataView = new CSVDataViewImpl();
        }
        return csvDataView;
    }
}
