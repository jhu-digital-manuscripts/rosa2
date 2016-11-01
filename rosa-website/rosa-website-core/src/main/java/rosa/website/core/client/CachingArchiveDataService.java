package rosa.website.core.client;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import rosa.archive.model.ImageList;
import rosa.website.model.select.BookSelectList;
import rosa.website.model.select.SelectCategory;
import rosa.website.model.table.Table;
import rosa.website.model.table.Tables;
import rosa.website.model.view.BookDescriptionViewModel;
import rosa.website.model.view.FSIViewerModel;

/**
 * An implementation of the ArchiveDataServiceAsync interface that caches all
 * RPC calls requesting data. This class caches RPC results in the client.
 *
 * NOTE a HashMap is used instead of the usual ConcurrentHashMap because
 * there is no GWT emulation of a ConcurrentHashMap.
 */
public class CachingArchiveDataService implements ArchiveDataServiceAsync {
    public static final CachingArchiveDataService INSTANCE = new CachingArchiveDataService();
    private static final int MAX_CACHE_SIZE = 1000;

    private final ArchiveDataServiceAsync service;
    private final Map<String, Object> cache;

    private CachingArchiveDataService() {
        service = GWT.create(ArchiveDataService.class);
        cache = new HashMap<>();
    }

    @Override
    public void loadCSVData(String collection, String lang, Tables type, final AsyncCallback<Table> cb) {
        final String key = getKey(collection, type.toString(), lang, Table.class);

        Object data = cache.get(key);

        if (data != null) {
            cb.onSuccess((Table) data);
            return;
        }

        service.loadCSVData(collection, lang, type, new AsyncCallback<Table>() {
            @Override
            public void onFailure(Throwable caught) {
                cb.onFailure(caught);
            }

            @Override
            public void onSuccess(Table result) {
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
            cb.onSuccess((BookSelectList) cache.get(key));
            return;
        }

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
