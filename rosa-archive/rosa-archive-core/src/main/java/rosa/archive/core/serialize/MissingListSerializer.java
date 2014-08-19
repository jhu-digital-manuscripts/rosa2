package rosa.archive.core.serialize;

import org.apache.commons.io.IOUtils;
import rosa.archive.core.RoseConstants;
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

    MissingListSerializer() {  }

    @Override
    public MissingList read(InputStream is) throws IOException {
        MissingList missingList = new MissingList();
        List<String> list = missingList.getMissing();

        List<String> lines = IOUtils.readLines(is, RoseConstants.CHARSET);
        for (String line : lines) {
            String[] parts = line.split(":");

            // BookID/BookID.order.txt:idOfMissingImage
            if (parts.length != 2) {
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
