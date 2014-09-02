package rosa.archive.core.serialize;

import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;
import rosa.archive.core.config.AppConfig;
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

    private AppConfig config;

    @Inject
    ChecksumInfoSerializer(AppConfig config) {
        this.config = config;
    }

    @Override
    public ChecksumInfo read(InputStream is, List<String> errors) throws IOException {
        ChecksumInfo info = new ChecksumInfo();

        List<String> lines = IOUtils.readLines(is, config.getCHARSET());
        for (String line : lines) {
            // Split on space
            String[] parts = line.split("\\s+");

            if (parts.length != 2) {
                errors.add("Malformed line in checksum data: [" + line + "]");
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
