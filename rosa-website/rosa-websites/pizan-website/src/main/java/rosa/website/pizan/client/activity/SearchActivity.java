package rosa.website.pizan.client.activity;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import rosa.search.model.QueryOperation;
import rosa.website.core.client.ArchiveDataServiceAsync;
import rosa.website.core.client.ClientFactory;
import rosa.website.core.client.place.AdvancedSearchPlace;
import rosa.website.core.client.view.AdvancedSearchView;
import rosa.website.core.client.widget.LoadingPanel;
import rosa.website.model.csv.CSVData;
import rosa.website.model.csv.CSVRow;
import rosa.website.model.csv.CSVType;
import rosa.website.model.csv.CollectionCSV;
import rosa.website.model.csv.CollectionCSV.Column;
import rosa.website.model.select.BookInfo;
import rosa.website.pizan.client.WebsiteConfig;
import rosa.website.search.client.SearchCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SearchActivity implements Activity {
    private static final Logger LOG = Logger.getLogger(SearchActivity.class.toString());

    private final AdvancedSearchPlace place;
    private final AdvancedSearchView view;
    private final ArchiveDataServiceAsync archiveDataService;

    /**
     * @param place initial search state
     * @param clientFactory .
     */
    public SearchActivity(AdvancedSearchPlace place, ClientFactory clientFactory) {
        this.place = place;
        this.view = clientFactory.advancedSearchView();
        this.archiveDataService = clientFactory.archiveDataService();
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

        String collection = WebsiteConfig.INSTANCE.collection();
        String lang = LocaleInfo.getCurrentLocale().getLocaleName();

        archiveDataService.loadCSVData(collection, lang, CSVType.COLLECTION_DATA, new AsyncCallback<CSVData>() {
            @Override
            public void onFailure(Throwable caught) {
                LOG.log(Level.SEVERE, "Failed to get book data.", caught);
            }

            @Override
            public void onSuccess(CSVData result) {
                if (result instanceof CollectionCSV) {
                    setSearchModel((CollectionCSV) result);
                } else {
                    LOG.log(Level.SEVERE, "Cannot initialize search widget, bad data returned from server.");
                }
                LoadingPanel.INSTANCE.hide();
            }
        });
    }

    private void setSearchModel(CollectionCSV data) {
        view.setAddFieldButtonText("Add Field");
        view.setSearchButtonText("Search");
        view.setRemoveButtonText("Remove");
        view.setClearBooksButtonText("Clear");

        view.setAvailableSearchFields(SearchCategory.values());
        view.setAvailableSearchOperations(QueryOperation.values());

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

        List<BookInfo> books = new ArrayList<>();
        for (CSVRow row : data) {
            books.add(new BookInfo(row.getValue(Column.NAME), row.getValue(Column.ID)));
        }
        view.addBooksToRestrictionList(books.toArray(new BookInfo[books.size()]));
    }
}
