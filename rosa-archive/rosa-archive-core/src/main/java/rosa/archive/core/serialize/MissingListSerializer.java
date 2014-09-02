package rosa.archive.core.serialize;

import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;
import rosa.archive.core.config.AppConfig;
import rosa.archive.model.MissingList;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * @see rosa.archive.model.MissingList
 *
 * TODO I'm not sure this list of missing images was used in rosa1
 */
public class MissingListSerializer implements Serializer<MissingList> {

    private AppConfig config;

    @Inject
    MissingListSerializer(AppConfig config) {
        this.config = config;
    }

    @Override
    public MissingList read(InputStream is, List<String> errors) throws IOException {
        MissingList missingList = new MissingList();
        List<String> list = missingList.getMissing();

        List<String> lines = IOUtils.readLines(is, config.getCHARSET());
        for (String line : lines) {
            String[] parts = line.split(":");

            // BookID/BookID.order.txt:idOfMissingImage
            if (parts.length != 2) {
                errors.add("Malformed line: [" + line + "]. Should have only 2 columns, instead has ("
                        + parts.length + ")");
                continue;
            }
            list.add(parts[1]);
        }

        return missingList;
    }

    @Override
    public void write(MissingList object, OutputStream out) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }
}
