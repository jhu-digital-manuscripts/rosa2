package rosa.website.rose.client.activity;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import rosa.website.core.client.ClientFactory;
import rosa.website.core.client.place.AdvancedSearchPlace;
import rosa.website.core.client.view.AdvancedSearchView;
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
    }

    @Override
    public void onStop() {
        view.clear();
    }

    @Override
    public void start(AcceptsOneWidget panel, EventBus eventBus) {
        panel.setWidget(view);

        setFakeSearchModel();
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
    }
}
