package rosa.website.core.server;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

import java.io.IOException;

public interface StoreAccessLayer {
    BookCollection collection(String collection) throws IOException;
    Book book(String collection, String book) throws IOException;
}
