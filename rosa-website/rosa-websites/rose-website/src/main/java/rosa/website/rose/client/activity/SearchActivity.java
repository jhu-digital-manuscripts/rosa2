package rosa.website.rose.client.activity;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import rosa.search.model.QueryOperation;
import rosa.search.model.QueryTerm;
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
import rosa.website.rose.client.WebsiteConfig;
import rosa.website.search.client.QueryUtil;
import rosa.website.search.client.RosaQueryUtil;
import rosa.website.search.client.SearchCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SearchActivity implements Activity {
    private static final Logger LOG = Logger.getLogger(SearchActivity.class.toString());
    private static final QueryUtil QUERY_UTIL = new RosaQueryUtil();

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

        init(books);
    }

    private void init(List<BookInfo> books) {
        if (place == null || place.getSearchToken() == null || place.getSearchToken().isEmpty()) {
            view.addQueryField();
            view.addQueryField();
            view.addQueryField();

            return;
        }
        final int OPERATION_AND = QueryOperation.AND.ordinal();
        final int CATEGORY_ALL = SearchCategory.ALL.ordinal();

        List<QueryTerm> terms = QUERY_UTIL.queryParts(place.getSearchToken());
        for (QueryTerm term : terms) {
            SearchCategory category = SearchCategory.category(term.getField());
            view.addQueryField(term.getValue(), OPERATION_AND, category == null ? CATEGORY_ALL : category.ordinal());
        }

        String[] restrictedBooks = QUERY_UTIL.bookRestrictionList(place.getSearchToken());
        for (String book : restrictedBooks) {
            BookInfo bookInfo = getBook(book, books);
            if (bookInfo != null) {
                view.setBooksAsRestricted(bookInfo);
            }
        }
    }

    private BookInfo getBook(String book, List<BookInfo> books) {
        for (BookInfo b : books) {
            if (b.id.equals(book) || b.title.equals(book)) {

                return b;
            }
        }

        return null;
    }
}
