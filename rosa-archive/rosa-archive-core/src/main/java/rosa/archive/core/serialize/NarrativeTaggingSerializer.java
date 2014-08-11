package rosa.archive.core.serialize;

import rosa.archive.model.BookScene;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 *
 */
public class NarrativeTaggingSerializer implements Serializer<List<BookScene>> {

    @Override
    public List<BookScene> read(InputStream is) {
        return null;
    }

    @Override
    public void write(List<BookScene> object, OutputStream out) {

    }
}
