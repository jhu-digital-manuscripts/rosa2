package rosa.archive.core.serialize;

import org.apache.commons.io.IOUtils;
import rosa.archive.core.RoseConstants;
import rosa.archive.model.ChecksumData;
import rosa.archive.model.ChecksumInfo;
import rosa.archive.model.HashAlgorithm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * @see rosa.archive.model.ChecksumInfo
 */
public class ChecksumInfoSerializer implements Serializer<ChecksumInfo> {
    private static final String DELIMITER = "  ";

    @Override
    public ChecksumInfo read(InputStream is) throws IOException {
        ChecksumInfo info = new ChecksumInfo();

        List<String> lines = IOUtils.readLines(is, RoseConstants.CHARSET);
        for (String line : lines) {
            // Split on space
            String[] parts = line.split("\\s+");

            if (parts.length != 2) {
                // TODO log this as an error
                continue;
            }
            ChecksumData data = new ChecksumData();
            data.setId(parts[1]);
            data.setHash(parts[0]);
            data.setAlgorithm(HashAlgorithm.SHA1);

            info.addChecksum(data);
        }

        return info;
    }

    @Override
    public void write(ChecksumInfo object, OutputStream out) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }
}
