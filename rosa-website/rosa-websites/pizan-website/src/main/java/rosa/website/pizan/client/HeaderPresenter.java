package rosa.website.pizan.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import rosa.website.core.client.*;
import rosa.website.core.client.place.AdvancedSearchPlace;
import rosa.website.core.client.view.HeaderView;
import rosa.website.core.client.view.HeaderView.Presenter;

public class HeaderPresenter implements Presenter, IsWidget {

    private final HeaderView view;

    /**
     * @param clientFactory .
     */
    public HeaderPresenter(final ClientFactory clientFactory) {
        final Labels labels = Labels.INSTANCE;

        this.view = clientFactory.headerView();
        view.setPresenter(this);

        view.addHeaderImage(GWT.getModuleBaseURL() + "header-5.jpg", labels.headerAlt());

        view.setSearchButtonText(labels.search());
        view.addAdvancedSearchLink(labels.advancedSearch(), "search;");
        view.addSearchClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                String searchToken = view.getSearchToken();

                if (searchToken != null && !searchToken.isEmpty()) {
                    clientFactory.placeController().goTo(new AdvancedSearchPlace(searchToken));
                }
            }
        });
    }

    @Override
    public Widget asWidget() {
        return view.asWidget();
    }

    @Override
    public void goHome() {
        History.newItem("home");
    }
}
