package rosa.website.rose.client.activity;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import rosa.search.model.Query;
import rosa.search.model.QueryOperation;
import rosa.search.model.QueryTerm;
import rosa.search.model.SearchMatch;
import rosa.search.model.SearchOptions;
import rosa.search.model.SearchResult;
import rosa.website.core.client.ArchiveDataServiceAsync;
import rosa.website.core.client.ClientFactory;
import rosa.website.core.client.FSIUtil;
import rosa.website.core.client.Labels;
import rosa.website.core.client.place.AdvancedSearchPlace;
import rosa.website.core.client.view.AdvancedSearchView;
import rosa.website.core.client.widget.LoadingPanel;
import rosa.website.model.csv.BookDataCSV;
import rosa.website.model.csv.BookDataCSV.Column;
import rosa.website.model.csv.CSVData;
import rosa.website.model.csv.CSVRow;
import rosa.website.model.csv.CSVType;
import rosa.website.model.select.BookInfo;
import rosa.website.rose.client.WebsiteConfig;
import rosa.website.search.client.QueryUtil;
import rosa.website.search.client.RosaQueryUtil;
import rosa.website.core.client.RosaSearchServiceAsync;
import rosa.website.search.client.model.SearchCategory;
import rosa.website.search.client.model.SearchMatchModel;
import rosa.website.search.client.model.SearchResultModel;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SearchActivity implements Activity {
    private static final Logger LOG = Logger.getLogger(SearchActivity.class.toString());
    private static final QueryUtil QUERY_UTIL = new RosaQueryUtil();
    private static final int MATCH_COUNT = 20;// Number of matches to return from search service. Can be used with paging.
    private static final int THUMB_WIDTH = 64;
    private static final int THUMB_HEIGHT = 64;

    private final AdvancedSearchPlace place;
    private final AdvancedSearchView view;
    private final ArchiveDataServiceAsync archiveDataService;
    private final RosaSearchServiceAsync searchService;

    private String resumeToken = null;     // For use in paging
    private BookDataCSV collection;

    /**
     * @param place initial search state
     * @param clientFactory .
     */
    public SearchActivity(AdvancedSearchPlace place, ClientFactory clientFactory) {
        this.place = place;
        this.view = clientFactory.advancedSearchView();
        this.archiveDataService = clientFactory.archiveDataService();
        this.searchService = clientFactory.searchService();
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

        initView();

        String collection = WebsiteConfig.INSTANCE.collection();
        String lang = LocaleInfo.getCurrentLocale().getLocaleName();

        archiveDataService.loadCSVData(collection, lang, CSVType.COLLECTION_BOOKS, new AsyncCallback<CSVData>() {
            @Override
            public void onFailure(Throwable caught) {
                LOG.log(Level.SEVERE, "Failed to get book data.", caught);
            }

            @Override
            public void onSuccess(CSVData result) {
                if (result instanceof BookDataCSV) {
                    setSearchModel((BookDataCSV) result);
                } else {
                    LOG.log(Level.SEVERE, "Cannot initialize search widget, bad data returned from server.");
                }
                LoadingPanel.INSTANCE.hide();
            }
        });
    }

    private void initView() {
        view.setAddFieldButtonText(Labels.INSTANCE.addSearchField());
        view.setSearchButtonText(Labels.INSTANCE.search());
        view.setRemoveButtonText(Labels.INSTANCE.removeSearchField());
        view.setClearBooksButtonText(Labels.INSTANCE.clearTextBox());

        view.setAvailableSearchFields(SearchCategory.values());
        view.setAvailableSearchOperations(QueryOperation.values());

        view.addSearchButtonClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                String query = view.getSearchQuery();       // This query string will have to be URL encoded first
                if (query != null && !query.isEmpty()) {
                    LOG.info("A search will happen now. Token: #" + view.getSearchQuery());
                    // Do search
                    performSearch(query);
                }
            }
        });
    }

    private void setSearchModel(BookDataCSV data) {
        this.collection = data;
        LOG.info("Books csv:\n" + data.toString());

        List<BookInfo> books = new ArrayList<>();
        for (CSVRow row : data) {
            books.add(new BookInfo(row.getValue(Column.COMMON_NAME), row.getValue(Column.ID)));
        }
        view.addBooksToRestrictionList(books.toArray(new BookInfo[books.size()]));

        setData(books);
    }

    private void setData(List<BookInfo> books) {
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

        performSearch(place.getSearchToken());
    }

    private BookInfo getBook(String book, List<BookInfo> books) {
        for (BookInfo b : books) {
            if (b.id.equals(book) || b.title.equals(book)) {

                return b;
            }
        }

        return null;
    }

    private void performSearch(String searchToken) {
        Query query = QUERY_UTIL.toQuery(searchToken);
        SearchOptions options = new SearchOptions(QUERY_UTIL.offset(searchToken), MATCH_COUNT, resumeToken);

        if (query == null) {
            return;
        }

        LOG.info("Performing search. [" + searchToken + "]");
        searchService.search(query, options, new AsyncCallback<SearchResult>() {
            @Override
            public void onFailure(Throwable caught) {
                LOG.log(Level.SEVERE, "Search failed.", caught);
            }

            @Override
            public void onSuccess(SearchResult result) {
                view.setResults(adaptSearchResults(result));
            }
        });
    }

    private SearchResultModel adaptSearchResults(SearchResult result) {
        SearchResultModel model = new SearchResultModel(result);

        for (SearchMatch match : result.getMatches()) {
            String pageId = QUERY_UTIL.getPageID(match);
            String bookId = QUERY_UTIL.getBookID(match);

            String fsiUrl;
            String targetUrl = GWT.getHostPageBaseURL();

            if (pageId == null || pageId.isEmpty()) {
                targetUrl += "#book;" + bookId;
                fsiUrl = null;
            } else {
                targetUrl += "#read;" + pageId;
                fsiUrl = FSIUtil.getFSIImageUrl(
                        WebsiteConfig.INSTANCE.fsiShare(),
                        bookId,
                        pageId,
                        THUMB_WIDTH,
                        THUMB_HEIGHT,
                        WebsiteConfig.INSTANCE.fsiUrl()
                );
            }

            model.addSearchMatch(new SearchMatchModel(match, fsiUrl, targetUrl, getDisplayName(bookId, pageId)));
        }

        return model;
    }

    private String getDisplayName(String bookId, String pageId) {
        if (pageId != null) {
            CSVRow row = collection.getRow(bookId);

            if (row != null) {
                return pageId + ": "
                        + row.getValue(Column.REPO) + " "
                        + row.getValue(Column.SHELFMARK);
            }
        }

        return bookId;
    }
}
