package rosa.website.core.server;

import rosa.archive.core.Store;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

import java.io.IOException;

interface StoreAccessLayer {
    boolean hasCollection(String name);
    boolean hasBook(String collection, String book);
    BookCollection collection(String collection) throws IOException;
    Book book(String collection, String book) throws IOException;
    Store store();
}
