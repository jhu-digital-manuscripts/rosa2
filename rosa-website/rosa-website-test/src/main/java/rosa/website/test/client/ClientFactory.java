package rosa.website.test.client;

import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.resources.client.ExternalTextResource;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;
import rosa.website.core.client.BaseClientFactory;
import rosa.website.test.client.view.HTMLView;
import rosa.website.test.client.view.impl.HTMLViewImpl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ClientFactory extends BaseClientFactory {
    protected static EventBus event_bus = new SimpleEventBus();
    protected static PlaceController place_controller = new PlaceController(event_bus);
    private final Resources resources = Resources.INSTANCE;

    private static HTMLView htmlView;

    public final Set<String> htmlPages;

    public ClientFactory(String[] htmlPages) {
        this.htmlPages = new HashSet<>(Arrays.asList(htmlPages));
    }
//
//    public EventBus eventBus() {
//        return event_bus;
//    }
//
//    public PlaceController placeController() {
//        return place_controller;
//    }

    public EventBus eventBus() {
        return event_bus;
    }

    public PlaceController placeController() {
        return place_controller;
    }

    public HTMLView htmlView() {
        if (htmlView == null) {
            htmlView = new HTMLViewImpl();
        }

        return htmlView;
    }

    public ExternalTextResource getExternalHtml(String name) {
        if (!htmlPages.contains(name)) {
            return null;
        }

        ExternalTextResource resource = null;
        switch (name) {
            case "home":
                resource = resources.home();
                break;
            case "notHome":
                resource = resources.notHome();
                break;
            default:
                break;
        }

        return resource;
    }
}
