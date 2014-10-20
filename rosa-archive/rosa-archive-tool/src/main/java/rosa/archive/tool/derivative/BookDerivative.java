package rosa.archive.tool.derivative;

import rosa.archive.core.store.Store;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class BookDerivative extends AbstractDerivative {

    private String collection;
    private String book;

    public BookDerivative(String collection, String book, PrintStream report, Store store) {
        super(report, store);
        this.collection = collection;
        this.book = book;
    }

    @Override
    public void updateChecksum(boolean force) throws IOException {
        List<String> errors = new ArrayList<>();
        store.updateChecksum(collection, book, force, errors);

        if (errors.size() > 0) {
            reportError(errors.toArray(new String[errors.size()]));
        }
    }

}
