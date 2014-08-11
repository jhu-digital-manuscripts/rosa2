package rosa.archive.core.serialize;

import rosa.archive.model.MissingList;

import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 */
public class MissingListSerializer implements Serializer<MissingList> {
    @Override
    public MissingList read(InputStream is) {
        return null;
    }

    @Override
    public void write(MissingList object, OutputStream out) {

    }
}
