package rosa.archive.core.serialize;

import rosa.archive.model.Book;

import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 */
public class BookSerializer implements Serializer<Book> {

    @Override
    public Book read(InputStream is) {
        return null;
    }

    @Override
    public void write(Book object, OutputStream out) {

    }

}
