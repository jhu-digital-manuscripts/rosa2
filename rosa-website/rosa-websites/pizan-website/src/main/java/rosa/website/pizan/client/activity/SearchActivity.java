package rosa.website.pizan.client.activity;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import rosa.website.core.client.ClientFactory;
import rosa.website.core.client.place.AdvancedSearchPlace;
import rosa.website.core.client.view.AdvancedSearchView;
import rosa.website.core.client.widget.LoadingPanel;
import rosa.website.model.select.BookInfo;

public class SearchActivity implements Activity {

    private final AdvancedSearchPlace place;
    private final AdvancedSearchView view;

    /**
     * @param place initial search state
     * @param clientFactory .
     */
    public SearchActivity(AdvancedSearchPlace place, ClientFactory clientFactory) {
        this.place = place;
        this.view = clientFactory.advancedSearchView();
    }

    @Override
    public String mayStop() {
        return null;
    }

    @Override
    public void onCancel() {
        view.clear();
        LoadingPanel.INSTANCE.hide();
    }

    @Override
    public void onStop() {
        view.clear();
        LoadingPanel.INSTANCE.hide();
    }

    @Override
    public void start(AcceptsOneWidget panel, EventBus eventBus) {
        LoadingPanel.INSTANCE.show();
        panel.setWidget(view);

        setFakeSearchModel();
        LoadingPanel.INSTANCE.hide();
    }

    private void setFakeSearchModel() {
        view.setAddFieldButtonText("Add Field");
        view.setSearchButtonText("Search");
        view.setRemoveButtonText("Remove");
        view.setClearBooksButtonText("Clear");

        BookInfo[] books = new BookInfo[10];
        for (int i = 0; i < 10; i++) {
            books[i] = new BookInfo("Book " + i, "Book" + i);
        }
        view.addBooksToRestrictionList(books);

        String[] availableOps = {"AND", "OR"};
        String[] availableFields = {"Field 1", "Field 2", "Field 3", "Field 4"};
        view.setAvailableSearchFields(availableFields);
        view.setAvailableSearchOperations(availableOps);

        view.addQueryField();
        view.addQueryField();
        view.addQueryField();

        view.addSearchButtonClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                String query = view.getSearchQuery();       // This query string will have to be URL encoded first
                if (query == null || query.isEmpty()) {
                    Window.alert("No search will happen because no search query was found.");
                } else {
                    Window.alert("A search will happen now. Token: #" + view.getSearchQuery());
                }
            }
        });
    }
}
