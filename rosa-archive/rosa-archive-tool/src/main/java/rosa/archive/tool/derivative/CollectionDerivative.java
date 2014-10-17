package rosa.archive.tool.derivative;

import rosa.archive.core.store.Store;
import rosa.archive.model.BookCollection;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class CollectionDerivative {

    private PrintStream report;
    private Store store;
    private String collection;

    public CollectionDerivative(String collection, PrintStream report, Store store) {
        this.collection = collection;
        this.store = store;
        this.report = report;
    }

    protected void reportError(String message, Exception e) {
        report.println("[Error] " + message);
        e.printStackTrace(report);
    }

    protected void reportError(String ... errors) {
        for (String err : errors) {
            report.println("[Error] " + err);
        }
    }

    public void updateChecksum(boolean force) throws IOException {
        List<String> errors = new ArrayList<>();

        BookCollection bc = store.loadBookCollection(collection, errors);
        if (bc == null) {
            report.println("[Error] Failed to load book collection. (" + collection + ")");
        } else {
            store.updateChecksum(bc, force, errors);
        }

        if (errors.size() > 0) {
            reportError(errors.toArray(new String[errors.size()]));
        }
    }

}
