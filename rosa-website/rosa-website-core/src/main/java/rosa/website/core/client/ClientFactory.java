package rosa.website.core.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;
import rosa.website.core.client.view.AdvancedSearchView;
import rosa.website.core.client.view.BookDescriptionView;
import rosa.website.core.client.view.BookSelectView;
import rosa.website.core.client.view.FSIViewerView;
import rosa.website.core.client.view.CSVDataView;
import rosa.website.core.client.view.HTMLView;
import rosa.website.core.client.view.HeaderView;
import rosa.website.core.client.view.JSViewerView;
import rosa.website.core.client.view.SidebarView;
import rosa.website.core.client.view.impl.AdvancedSearchViewImpl;
import rosa.website.core.client.view.impl.BookDescriptionViewImpl;
import rosa.website.core.client.view.impl.BookSelectViewImpl;
import rosa.website.core.client.view.impl.FSIViewerViewImpl;
import rosa.website.core.client.view.impl.CSVDataViewImpl;
import rosa.website.core.client.view.impl.HTMLViewImpl;
import rosa.website.core.client.view.impl.HeaderViewImpl;
import rosa.website.core.client.view.impl.JSViewerViewImpl;
import rosa.website.core.client.view.impl.SidebarViewImpl;

public class ClientFactory {
    private static AppContext context = new AppContext();
    
    private static EventBus event_bus = new SimpleEventBus();
    private static PlaceController place_controller = new PlaceController(event_bus);
    private static ArchiveDataServiceAsync archiveDataService = CachingArchiveDataService.INSTANCE;
    private static RosaSearchServiceAsync searchService = GWT.create(RosaSearchService.class);

    private static HeaderView headerView = new HeaderViewImpl();

    public AppContext context() {
        return context;
    }

    public EventBus eventBus() {
        return event_bus;
    }

    public PlaceController placeController() {
        return place_controller;
    }

    public ArchiveDataServiceAsync archiveDataService() {
        return archiveDataService;
    }

    public RosaSearchServiceAsync searchService() {
        return searchService;
    }

    public HTMLView htmlView() {
        return new HTMLViewImpl();
    }

    public CSVDataView csvDataView() {
        return new CSVDataViewImpl();
    }

    public BookSelectView bookSelectView() {
        return new BookSelectViewImpl();
    }

    public BookDescriptionView bookDescriptionView() {
        return new BookDescriptionViewImpl();
    }

    public FSIViewerView bookViewerView() {
        return new FSIViewerViewImpl();
    }

    public JSViewerView jsViewerView() {
        return new JSViewerViewImpl();
    }

    public HeaderView headerView() {
        return headerView;
    }

    public SidebarView sidebarView() {
        return new SidebarViewImpl();
    }

    public AdvancedSearchView advancedSearchView() {
        return new AdvancedSearchViewImpl();
    }
}
