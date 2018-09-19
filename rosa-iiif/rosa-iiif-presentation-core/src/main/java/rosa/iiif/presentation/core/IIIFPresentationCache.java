package rosa.iiif.presentation.core;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.inject.Inject;

import rosa.archive.core.Store;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

/**
 * Caches up to a given number of objects in memory. Helpers are provided to
 * access cached Book and BookCollection objects and load them from a Store as
 * needed.
 */

public class IIIFPresentationCache {
    private final static Logger logger = Logger.getLogger(IIIFPresentationCache.class.getName());

    private final Store store;
    private final ConcurrentHashMap<String, Object> cache;
    private final int max_cache_size;

    /**
     * @param store
     * @param max_cache_size
     *            max number of objects to cache at a time
     */
    @Inject
    public IIIFPresentationCache(Store store, int max_cache_size) {
        this.store = store;
        this.max_cache_size = max_cache_size;
        this.cache = new ConcurrentHashMap<>();
    }

    /**
     * @param id
     *            uniquely identifies the object in the type
     * @param type
     * @param supplier
     *            If object not in cache, it is retrieved from the supplier and
     *            cached. The supplier must not itself call get. 
     * @return value in cache if present or updates cache with supplied value, null
     *         indicates no value
     */
    public <T> T get(String id, Class<T> type, Supplier<T> supplier) {
        if (cache.size() > max_cache_size) {
            cache.clear();
        }

        return type.cast(cache.computeIfAbsent(id + "," + type.getName(), k -> supplier.get()));
    }

    public BookCollection getBookCollection(String col_id) {
        return get(col_id, BookCollection.class, () -> {
            try {
                return store.loadBookCollection(col_id, null);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Loading collection " + col_id, e);
                return null;
            }
        });
    }

    public Book getBook(BookCollection book_col, String book_id) {
        String id = book_col.getId() + book_id;

        return get(id, Book.class, () -> {
            try {
                return store.loadBook(book_col, book_id, null);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Loading book " + book_col.getId() + " " + book_id, e);
                return null;
            }
        });
    }
}
