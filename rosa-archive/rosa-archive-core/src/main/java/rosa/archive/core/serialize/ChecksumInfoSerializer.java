package rosa.archive.core.serialize;

import rosa.archive.model.ChecksumInfo;

import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 */
public class ChecksumInfoSerializer implements Serializer<ChecksumInfo> {
    @Override
    public ChecksumInfo read(InputStream is) {
        return null;
    }

    @Override
    public void write(ChecksumInfo object, OutputStream out) {

    }
}
