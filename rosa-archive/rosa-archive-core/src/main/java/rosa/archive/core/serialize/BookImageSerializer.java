package rosa.archive.core.serialize;

import rosa.archive.model.BookImage;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * TODO need wrapper classes for book images + cropped images?
 */
public class BookImageSerializer implements Serializer<List<BookImage>> {
    @Override
    public List<BookImage> read(InputStream is) {
        return null;
    }

    @Override
    public void write(List<BookImage> object, OutputStream out) {

    }
}
