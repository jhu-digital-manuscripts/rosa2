package rosa.archive.core.serialize;

import rosa.archive.model.BookCollection;

import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 */
public class BookCollectionSerializer implements Serializer<BookCollection> {
    @Override
    public BookCollection read(InputStream is) {
        return null;
    }

    @Override
    public void write(BookCollection object, OutputStream out) {

    }
}
