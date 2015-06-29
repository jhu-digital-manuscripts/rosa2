package rosa.website.core.client;

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

    private static HeaderView headerView = new HeaderViewImpl();

    private static HTMLView htmlView = new HTMLViewImpl();
    private static CSVDataView csvDataView = new CSVDataViewImpl();
    private static BookSelectView bookSelectView = new BookSelectViewImpl();
    private static BookDescriptionView bookDescriptionView = new BookDescriptionViewImpl();
    private static FSIViewerView FSIViewerView = new FSIViewerViewImpl();
    private static JSViewerView jsViewerView = new JSViewerViewImpl();
    private static SidebarView sidebarView = new SidebarViewImpl();
    private static AdvancedSearchView advancedSearchView = new AdvancedSearchViewImpl();

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

    public HTMLView htmlView() {
        return htmlView;
    }

    public CSVDataView csvDataView() {
        return csvDataView;
    }

    public BookSelectView bookSelectView() {
        return bookSelectView;
    }

    public BookDescriptionView bookDescriptionView() {
        return bookDescriptionView;
    }

    public FSIViewerView bookViewerView() {
        return FSIViewerView;
    }

    public JSViewerView jsViewerView() {
        return jsViewerView;
    }

    public HeaderView headerView() {
        return headerView;
    }

    public SidebarView sidebarView() {
        return sidebarView;
    }

    public AdvancedSearchView advancedSearchView() {
        return advancedSearchView;
    }
}
