package rosa.website.core.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.website.model.csv.BookDataCSV;
import rosa.website.model.csv.CSVData;
import rosa.website.model.csv.CollectionCSV;
import rosa.website.model.csv.CsvType;
import rosa.website.model.csv.IllustrationTitleCSV;
import rosa.website.model.select.BookSelectList;
import rosa.website.model.select.SelectCategory;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class CachingArchiveDataService implements ArchiveDataServiceAsync {
    private static final Logger logger = Logger.getLogger(CachingArchiveDataService.class.toString());
    public static final CachingArchiveDataService INSTANCE = new CachingArchiveDataService();
    private static final int MAX_CACHE_SIZE = 1000;

    private final ArchiveDataServiceAsync service;
    private final Map<String, Object> cache;

    private CachingArchiveDataService() {
        service = GWT.create(ArchiveDataService.class);
        cache = new HashMap<>();
    }

    @Override
    public void loadCSVData(String collection, String lang, CsvType type, final AsyncCallback<CSVData> cb) {
        final String key = getKey(collection, type.toString(), lang, CSVData.class);
        if (inCache(key)) {
            logger.info("Found CSVData in cache (" + key + ") -> {" + cb.toString() + "}");
            cb.onSuccess(fromCache(key, CSVData.class));
            return;
        }

        logger.info("CSVData not found in cache. Loading from data service. (" + key + ")");
        service.loadCSVData(collection, lang, type, new AsyncCallback<CSVData>() {
            @Override
            public void onFailure(Throwable caught) {
                cb.onFailure(caught);
            }

            @Override
            public void onSuccess(CSVData result) {
                updateCache(key, result);
                cb.onSuccess(result);
            }
        });
    }

    @Override
    public void loadCollectionData(String collection, String lang, final AsyncCallback<CollectionCSV> cb) {
        final String key = getKey(collection, "", lang, CollectionCSV.class);
        if (inCache(key)) {
            logger.info("Found collection data in cache. (" + key + ")");
            cb.onSuccess(fromCache(key, CollectionCSV.class));
            return;
        }

        logger.info("Collection data not found in cache. Loading from data service. (" + key + ")");
        service.loadCollectionData(collection, lang, new AsyncCallback<CollectionCSV>() {
            @Override
            public void onFailure(Throwable caught) {
                cb.onFailure(caught);
            }

            @Override
            public void onSuccess(CollectionCSV result) {
                updateCache(key, result);
                cb.onSuccess(result);
            }
        });
    }

    @Override
    public void loadCollectionBookData(String collection, String lang, final AsyncCallback<BookDataCSV> cb) {
        final String key = getKey(collection, "", lang, BookDataCSV.class);
        if (inCache(key)) {
            logger.info("Found collection book data in cache. (" + key + ")");
            cb.onSuccess(fromCache(key, BookDataCSV.class));
            return;
        }

        logger.info("Collection book data not found in cache. Loading from data service. (" + key + ")");
        service.loadCollectionBookData(collection, lang, new AsyncCallback<BookDataCSV>() {
            @Override
            public void onFailure(Throwable caught) {
                cb.onFailure(caught);
            }

            @Override
            public void onSuccess(BookDataCSV result) {
                updateCache(key, result);
                cb.onSuccess(result);
            }
        });
    }

    @Override
    public void loadBookSelectionData(String collection, SelectCategory category, String lang,
                                      final AsyncCallback<BookSelectList> cb) {
        final String key = getKey(collection, category.toString(), lang, BookSelectList.class);
        if (inCache(key)) {
            logger.info("Found book selection data in cache. (" + key + ")");
            cb.onSuccess(fromCache(key, BookSelectList.class));
            return;
        }

        logger.info("Book selection data not found in cache. Loading from data service. (" + key + ")");
        service.loadBookSelectionData(collection, category, lang, new AsyncCallback<BookSelectList>() {
            @Override
            public void onFailure(Throwable caught) {
                cb.onFailure(caught);
            }

            @Override
            public void onSuccess(BookSelectList result) {
                updateCache(key, result);
                cb.onSuccess(result);
            }
        });
    }

    @Override
    public void loadIllustrationTitles(String collection, final AsyncCallback<IllustrationTitleCSV> cb) {
        final String key = getKey(collection, "illustration_titles.csv", "", IllustrationTitleCSV.class);
        if (inCache(key)) {
            logger.info("Illustration titles CSV found in cache. (" + key + ")");
            cb.onSuccess(fromCache(key, IllustrationTitleCSV.class));
            return;
        }

        logger.info("Illustration titles not found in cache. Loading from data service. (" + key + ")");
        service.loadIllustrationTitles(collection, new AsyncCallback<IllustrationTitleCSV>() {
            @Override
            public void onFailure(Throwable caught) {
                cb.onFailure(caught);
            }

            @Override
            public void onSuccess(IllustrationTitleCSV result) {
                updateCache(key, result);
                cb.onSuccess(result);
            }
        });
    }

    @Override
    public void loadBookCollection(String collection, final AsyncCallback<BookCollection> cb) {
        final String key = getKey(collection, "", "", BookCollection.class);
        if (inCache(key)) {
            logger.info("Found BookCollection in cache. (" + key + ")");
            cb.onSuccess(fromCache(key, BookCollection.class));
            return;
        }

        logger.info("Book collection not found in cache. Loading from data service. (" + key + ")");
        service.loadBookCollection(collection, new AsyncCallback<BookCollection>() {
            @Override
            public void onFailure(Throwable caught) {
                cb.onFailure(caught);
            }

            @Override
            public void onSuccess(BookCollection result) {
                updateCache(key, result);
                cb.onSuccess(result);
            }
        });
    }

    @Override
    public void loadBook(String collection, String book, final AsyncCallback<Book> cb) {
        final String key = getKey(collection, book, "", Book.class);
        if (inCache(key)) {
            logger.info("Book found in cache. (" + key + ")");
            cb.onSuccess(fromCache(key, Book.class));
            return;
        }

        logger.info("Book not found in cache. Loading from data service. (" + key + ")");
        service.loadBook(collection, book, new AsyncCallback<Book>() {
            @Override
            public void onFailure(Throwable caught) {
                cb.onFailure(caught);
            }

            @Override
            public void onSuccess(Book result) {
                updateCache(key, result);
                cb.onSuccess(result);
            }
        });
    }

    @Override
    public void loadPermissionStatement(String collection, String book, String lang, final AsyncCallback<String> cb) {
        final String key = getKey(collection, book, lang, String.class);
        if (inCache(key)) {
            cb.onSuccess(fromCache(key, String.class));
            return;
        }

        service.loadPermissionStatement(collection, book, lang, new AsyncCallback<String>() {
            @Override
            public void onFailure(Throwable caught) {
                cb.onFailure(caught);
            }

            @Override
            public void onSuccess(String result) {
                updateCache(key, result);
                cb.onSuccess(result);
            }
        });
    }

    public void clearCache() {
        cache.clear();
    }

    private <T> String getKey(String collection, String name, String lang, Class<T> type) {
        return collection + ";" + name + ";" + lang + ";" + type.getName();
    }

    private boolean inCache(String key) {
        return cache.containsKey(key);
    }

    @SuppressWarnings("unchecked")
    private <T> T fromCache(String key, Class<T> type) {
        Object result = cache.get(key);
        return (T) result;
    }

    private void updateCache(String key, Object value) {
        if (cache.size() > MAX_CACHE_SIZE) {
            clearCache();
        }

        cache.put(key, value);
    }
}
