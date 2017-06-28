package rosa.archive.core;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

import java.io.IOException;
import java.util.List;

/**
 * Simplified version of {@link Store} that is concerned only with
 * loading top level data objects, {@link Book} and {@link BookCollection}
 */
public interface SimpleStore {
    List<String> listCollections() throws IOException;

    /**
     * Load a book collection. "top" is a special collection name that
     * contains all collections in the archive.
     *
     * @param collection collection archive name
     * @return BookCollection object
     * @throws IOException .
     */
    BookCollection loadBookCollection(String collection) throws IOException;

    /**
     * Load a book within a collection.
     *
     * @param collection collection archive name
     * @param book book archive name within the collection
     * @return Book object
     * @throws IOException .
     */
    Book loadBook(String collection, String book) throws IOException;
}
