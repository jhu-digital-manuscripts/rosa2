package rosa.website.core.client.mvp;

import com.google.gwt.place.shared.PlaceHistoryHandler;
import rosa.website.core.client.ClientFactory;
import rosa.website.core.client.SidebarPresenter;
import rosa.website.core.client.event.BookSelectEvent;
import rosa.website.core.client.event.BookSelectEventHandler;
import rosa.website.core.client.event.FlashStatusChangeEvent;
import rosa.website.core.client.event.FlashStatusChangeEventHandler;
import rosa.website.core.client.event.LangaugeChangeEventHandler;
import rosa.website.core.client.event.LanguageChangeEvent;

import java.util.logging.Logger;

public class AppController implements FlashStatusChangeEventHandler, LangaugeChangeEventHandler,
        BookSelectEventHandler {
    private static final Logger logger = Logger.getLogger(AppController.class.toString());

    private final ClientFactory clientFactory;
    private final SidebarPresenter sidebarPresenter;
    private final PlaceHistoryHandler historyHandler;

    public AppController(SidebarPresenter sidebarPresenter, PlaceHistoryHandler historyHandler,
                         ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        this.sidebarPresenter = sidebarPresenter;
        this.historyHandler = historyHandler;
    }

    @Override
    public void onFlashStatusChange(FlashStatusChangeEvent event) {
        // Update context
        clientFactory.context().setUseFlash(event.status());
        historyHandler.handleCurrentHistory();
    }

    @Override
    public void onLanguageChange(LanguageChangeEvent event) {
        // Update context

        clientFactory.context().setLanguage(event.getLanguage());
        historyHandler.handleCurrentHistory();
    }

    @Override
    public void onBookSelect(BookSelectEvent event) {
        // Must update the sidebar to add/remove a new 'Book' section
        // Includes links to:
        //   * Description
        //   * Page Turner
        //   * Browser images
        if (event.isSelected()) {
            sidebarPresenter.addBookLinks(event.getBookId());
        } else {
            sidebarPresenter.clearBookLinks();
        }
    }
}
