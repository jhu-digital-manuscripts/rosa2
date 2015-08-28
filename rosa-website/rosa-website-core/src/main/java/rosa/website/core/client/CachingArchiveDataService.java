package rosa.website.core.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import rosa.archive.model.ImageList;
import rosa.website.model.view.BookDescriptionViewModel;
import rosa.website.model.view.FSIViewerModel;
import rosa.website.model.csv.BookDataCSV;
import rosa.website.model.csv.CSVData;
import rosa.website.model.csv.CSVType;
import rosa.website.model.csv.CollectionCSV;
import rosa.website.model.csv.IllustrationTitleCSV;
import rosa.website.model.select.BookSelectList;
import rosa.website.model.select.SelectCategory;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * An implementation of the ArchiveDataServiceAsync interface that caches all
 * RPC calls requesting data. This class caches RPC results in the client.
 *
 * NOTE a HashMap is used instead of the usual ConcurrentHashMap because
 * there is no GWT emulation of a ConcurrentHashMap.
 */
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
    public void loadCSVData(String collection, String lang, CSVType type, final AsyncCallback<CSVData> cb) {
        final String key = getKey(collection, type.toString(), lang, CSVData.class);

        Object data = cache.get(key);
        if (data != null) {
            logger.info("Found CSVData in cache (" + key + ") -> {" + cb.toString() + "}");
            cb.onSuccess((CSVData) data);
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

        Object data = cache.get(key);
        if (data != null) {
            logger.info("Found collection data in cache. (" + key + ")");
            cb.onSuccess((CollectionCSV) cache.get(key));
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

        Object data = cache.get(key);
        if (data != null) {
            logger.info("Found collection book data in cache. (" + key + ")");
            cb.onSuccess((BookDataCSV) cache.get(key));
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

        Object data = cache.get(key);
        if (data != null) {
            logger.info("Found book selection data in cache. (" + key + ")");
            cb.onSuccess((BookSelectList) cache.get(key));
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

        Object data = cache.get(key);
        if (data != null) {
            logger.info("Illustration titles CSV found in cache. (" + key + ")");
            cb.onSuccess((IllustrationTitleCSV) cache.get(key));
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
    public void loadPermissionStatement(String collection, String book, String lang, final AsyncCallback<String> cb) {
        final String key = getKey(collection, book, lang, String.class);

        Object data = cache.get(key);
        if (data != null) {
            cb.onSuccess((String) cache.get(key));
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

    @Override
    public void loadImageListAsString(String collection, String book, final AsyncCallback<String> cb) {
        final String key = getKey(collection, book, "", String.class);

        Object data = cache.get(key);
        if (data != null) {
            cb.onSuccess((String) cache.get(key));
            return;
        }

        service.loadImageListAsString(collection, book, new AsyncCallback<String>() {
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

    @Override
    public void loadImageList(String collection, String book, final AsyncCallback<ImageList> cb) {
        final String key = getKey(collection, book, "", ImageList.class);

        Object data = cache.get(key);
        if (data != null) {
            cb.onSuccess((ImageList) cache.get(key));
            return;
        }

        service.loadImageList(collection, book, new AsyncCallback<ImageList>() {
            @Override
            public void onFailure(Throwable caught) {
                cb.onFailure(caught);
            }

            @Override
            public void onSuccess(ImageList result) {
                updateCache(key, result);
                cb.onSuccess(result);
            }
        });
    }

    @Override
    public void loadFSIViewerModel(String collection, String book, String language, final AsyncCallback<FSIViewerModel> cb) {
        final String key = getKey(collection, book, "", FSIViewerModel.class);

        Object data = cache.get(key);
        if (data != null) {
            cb.onSuccess((FSIViewerModel) data);
            return;
        }

        service.loadFSIViewerModel(collection, book, language, new AsyncCallback<FSIViewerModel>() {
            @Override
            public void onFailure(Throwable caught) {
                cb.onFailure(caught);
            }

            @Override
            public void onSuccess(FSIViewerModel result) {
                updateCache(key, result);
                cb.onSuccess(result);
            }
        });
    }

    @Override
    public void loadBookDescriptionModel(String collection, String book, String language,
                                         final AsyncCallback<BookDescriptionViewModel> cb) {
        final String key = getKey(collection, book, language, BookDescriptionViewModel.class);

        Object data = cache.get(key);
        if (data != null) {
            cb.onSuccess((BookDescriptionViewModel) data);
            return;
        }

        service.loadBookDescriptionModel(collection, book, language, new AsyncCallback<BookDescriptionViewModel>() {
            @Override
            public void onFailure(Throwable caught) {
                cb.onFailure(caught);
            }

            @Override
            public void onSuccess(BookDescriptionViewModel result) {
                updateCache(key, result);
                cb.onSuccess(result);
            }
        });
    }

    /**  */
    public void clearCache() {
        cache.clear();
    }

    private <T> String getKey(String collection, String name, String lang, Class<T> type) {
        return collection + ";" + name + ";" + lang + ";" + type.getName();
    }

    private void updateCache(String key, Object value) {
        if (cache.size() > MAX_CACHE_SIZE) {
            clearCache();
        }

        cache.put(key, value);
    }
}
