package rosa.archive.core;

import com.google.inject.Inject;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of a {@link SimpleStore} that uses naive caching
 * to ease data access.
 */
public class SimpleCachingStore implements SimpleStore {

    private final Store store;
    private final int max_cache_size;

    private final ConcurrentHashMap<String, Object> cache;

    @Inject
    public SimpleCachingStore(Store store, int max_cache_size) {
        this.store = store;
        this.max_cache_size = max_cache_size;
        this.cache = new ConcurrentHashMap<>();
    }

    @Override
    public List<String> listCollections() throws IOException {
        return Arrays.asList(store.listBookCollections());
    }

    @Override
    public BookCollection loadBookCollection(String collection) throws IOException {
        return load_book_collection(collection);
    }

    @Override
    public Book loadBook(String collection, String book) throws IOException {
        return load_book(collection, book);
    }

    /**
     * Id of object in cache must be unique for class.
     *
     * @param id object ID
     * @param type object class type
     * @return a cache identifier for the object
     */
    private String cache_key(String id, Class<?> type) {
        return id + "," + type.getName();
    }

    private <T> T lookupCache(String id, Class<T> type) {
        return type.cast(cache.get(cache_key(id, type)));
    }

    private void updateCache(String id, Object value) {
        if (id == null || value == null) {
            return;
        }

        if (cache.size() > max_cache_size) {
            cache.clear();
        }

        cache.putIfAbsent(cache_key(id, value.getClass()), value);
    }

    private BookCollection load_book_collection(String col_id) throws IOException {
        BookCollection result = lookupCache(col_id, BookCollection.class);

        if (result == null) {
            result = store.loadBookCollection(col_id, null);
            updateCache(col_id, result);
        }

        return result;
    }

    private Book load_book(String col_id, String book_id) throws IOException {
        Book result = lookupCache(book_id, Book.class);

        if (result == null) {
            BookCollection col = load_book_collection(col_id);

            if (col == null) {
                return null;
            }

            result = store.loadBook(col, book_id, null);
            updateCache(book_id, result);
        }

        return result;
    }
}
