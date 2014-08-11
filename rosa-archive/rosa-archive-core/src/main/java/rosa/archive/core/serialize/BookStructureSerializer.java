package rosa.archive.core.serialize;

import rosa.archive.model.BookStructure;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Associated with Reduced Tagging.
 */
public class BookStructureSerializer implements Serializer<BookStructure> {
    @Override
    public BookStructure read(InputStream is) {
        return null;
    }

    @Override
    public void write(BookStructure object, OutputStream out) {

    }
}
