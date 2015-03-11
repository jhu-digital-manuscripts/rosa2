package rosa.website.test.client;

import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.web.bindery.event.shared.EventBus;
import rosa.website.test.client.nav.RosaActivityMapper;
import rosa.website.test.client.nav.DefaultRosaHistoryMapper;
import rosa.website.test.client.nav.RosaHistoryMapper;
import rosa.website.test.client.place.HTMLPlace;

import java.util.logging.Level;
import java.util.logging.Logger;

public class RosaWebsiteTest implements EntryPoint {
    private static final Logger logger = Logger.getLogger("");

    // Will be configured
    private static final String[] htmlPlaces = {"home", "notHome"};
    private static final String[] staticPlaces = {"one", "two"};

    /**
     * This is the default place that will load when the application
     * first starts up. When there is no history token to read, this
     * is the place that will be displayed.
     */
    private Place default_place = new HTMLPlace("home");

    private SimplePanel main_content = new SimplePanel();
    private final DockLayoutPanel main = new DockLayoutPanel(Style.Unit.PX);

    @Override
    public void onModuleLoad() {
        ClientFactory clientFactory = new ClientFactory(htmlPlaces);
        EventBus eventBus = clientFactory.eventBus();
        final PlaceController placeController = clientFactory.placeController();

        // Start ActivityManager for main widget with ActivityMapper
        ActivityMapper activity_mapper = new RosaActivityMapper(clientFactory);
        final ActivityManager activity_manager = new ActivityManager(activity_mapper, eventBus);
        activity_manager.setDisplay(main_content);

        DefaultRosaHistoryMapper history_mapper = GWT.create(DefaultRosaHistoryMapper.class);
        RosaHistoryMapper appHistoryMapper = new RosaHistoryMapper(history_mapper, htmlPlaces, staticPlaces);
        final PlaceHistoryHandler history_handler = new PlaceHistoryHandler(appHistoryMapper);
        history_handler.register(placeController, eventBus, default_place);

        history_handler.handleCurrentHistory();

        main.add(main_content);
        RootLayoutPanel.get().add(main);

        GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
            @Override
            public void onUncaughtException(Throwable e) {
                logger.log(Level.SEVERE, "Uncaught exception.", e);
                placeController.goTo(default_place);
            }
        });
    }
}
