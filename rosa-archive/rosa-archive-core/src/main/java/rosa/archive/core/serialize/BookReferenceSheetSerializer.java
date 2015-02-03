package rosa.archive.core.serialize;

import org.apache.commons.io.IOUtils;
import rosa.archive.core.util.CSV;
import rosa.archive.model.BookReferenceSheet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

public class BookReferenceSheetSerializer implements Serializer<BookReferenceSheet> {
    @Override
    public BookReferenceSheet read(InputStream is, List<String> errors) throws IOException {
        BookReferenceSheet reference = new BookReferenceSheet();

        List<String> lines = IOUtils.readLines(is, UTF_8);

        for (String line : lines) {
            String[] row = CSV.parse(line);
            if (row.length == 0 || row[0].equalsIgnoreCase("Standard title")) {
                continue;
            }

            reference.addValues(row[0]);
            for (int i = 1; i < row.length; i++) {
                if (i < 6) {
                    reference.addValues(row[0]);
                } else if (i == 6) {
                    // Either a single author, or comma-separated list of authors
                    reference.setAuthors(Arrays.asList(CSV.parse(row[i])));
                } else if (i == 7) {
                    reference.setFullTitle(row[i]);
                }
            }
        }

        return reference;
    }

    @Override
    public void write(BookReferenceSheet object, OutputStream out) throws IOException {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public Class<BookReferenceSheet> getObjectType() {
        return BookReferenceSheet.class;
    }
}
