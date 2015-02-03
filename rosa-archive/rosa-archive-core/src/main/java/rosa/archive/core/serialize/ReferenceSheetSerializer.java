package rosa.archive.core.serialize;

import org.apache.commons.io.IOUtils;
import rosa.archive.core.util.CSV;
import rosa.archive.model.ReferenceSheet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class ReferenceSheetSerializer implements Serializer<ReferenceSheet> {
    @Override
    public ReferenceSheet read(InputStream is, List<String> errors) throws IOException {
        ReferenceSheet reference = new ReferenceSheet();

        List<String> lines = IOUtils.readLines(is, UTF_8);

        for (String line : lines) {
            String[] row = CSV.parse(line);
            if (row.length == 0) {
                continue;
            }

            if (row.length == 1) {
                reference.addValues(row[0]);
            } else {
                reference.addValues(row[0], row);
            }
        }

        return reference;
    }

    @Override
    public void write(ReferenceSheet object, OutputStream out) throws IOException {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public Class<ReferenceSheet> getObjectType() {
        return ReferenceSheet.class;
    }
}
