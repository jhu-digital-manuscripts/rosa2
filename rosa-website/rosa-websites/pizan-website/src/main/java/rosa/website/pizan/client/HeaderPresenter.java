package rosa.website.pizan.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;
import rosa.website.core.client.*;
import rosa.website.core.client.event.SidebarItemSelectedEvent;
import rosa.website.core.client.view.HeaderViewNoSearch;
import rosa.website.core.client.view.HeaderViewNoSearch.Presenter;

import java.util.HashMap;
import java.util.Map;

public class HeaderPresenter implements Presenter, IsWidget {
    private Labels labels = Labels.INSTANCE;

    private final HeaderViewNoSearch view;
    private final EventBus eventBus;

    /**
     * @param clientFactory .
     */
    public HeaderPresenter(final ClientFactory clientFactory) {
        this.eventBus = clientFactory.eventBus();
        final Labels labels = Labels.INSTANCE;

        this.view = clientFactory.headerViewNoSearch();
        view.setPresenter(this);

        view.addHeaderImage(GWT.getModuleBaseURL() + "header-5.jpg", labels.headerAlt());
        addSiteNavLinks();
        addProjectLinks();

//        view.setSearchButtonText(labels.search());
//        view.addAdvancedSearchLink(labels.advancedSearch(), "search;");
//        view.addSearchClickHandler(new ClickHandler() {
//            @Override
//            public void onClick(ClickEvent event) {
//                String searchToken = view.getSearchToken();
//
//                if (searchToken != null && !searchToken.trim().isEmpty()) {
//                    clientFactory.placeController().goTo(new AdvancedSearchPlace(searchToken));
//                }
//            }
//        });
//
//        view.addSearchKeyPressHandler(new KeyPressHandler() {
//            @Override
//            public void onKeyPress(KeyPressEvent event) {
//                if (event.getUnicodeCharCode() != KeyCodes.KEY_ENTER) {
//                    return;
//                }
//
//                String searchToken = view.getSearchToken();
//                if (searchToken != null && !searchToken.trim().isEmpty()) {
//                    clientFactory.placeController().goTo(new AdvancedSearchPlace(searchToken));
//                }
//            }
//        });
    }

    @Override
    public Widget asWidget() {
        return view.asWidget();
    }

    @Override
    public void goHome() {
        eventBus.fireEvent(new SidebarItemSelectedEvent(null));
        History.newItem("home");
    }

    private void addProjectLinks() {
        Map<String, String> links = new HashMap<>();

        links.put(labels.partners(), "partners");
        links.put(labels.termsAndConditions(), "terms");
        links.put(labels.contactUs(), "contact");

        view.addNavMenu(labels.project(), links);
    }
    private void addSiteNavLinks() {
        view.addNavLink("home", "home");
        view.addNavLink(labels.whoIsPizan(), "pizan");
        view.addNavLink(labels.works(), "works");
        view.addNavLink(labels.properNames(), "names");
    }

}
