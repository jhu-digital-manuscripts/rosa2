package rosa.archive.core.serialize;

import rosa.archive.model.BookMetadata;

import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 */
public class BookMetadataSerializer implements Serializer<BookMetadata> {
    @Override
    public BookMetadata read(InputStream is) {
        return null;
    }

    @Override
    public void write(BookMetadata object, OutputStream out) {

    }
}
