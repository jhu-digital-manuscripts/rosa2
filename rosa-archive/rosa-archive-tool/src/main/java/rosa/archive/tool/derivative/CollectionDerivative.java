package rosa.archive.tool.derivative;

import rosa.archive.core.store.Store;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class CollectionDerivative extends AbstractDerivative {

    protected String collection;

    public CollectionDerivative(String collection, PrintStream report, Store store) {
        super(report, store);
        this.collection = collection;
    }

    public void updateChecksum(boolean force) throws IOException {
        List<String> errors = new ArrayList<>();

        store.updateChecksum(collection, force, errors);

        if (errors.size() > 0) {
            reportError(errors.toArray(new String[errors.size()]));
        }
    }

}
