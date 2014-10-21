package rosa.archive.tool.derivative;

import rosa.archive.core.store.Store;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

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

    @Override
    public void check(boolean checkBits) throws IOException {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> loadingErrors = new ArrayList<>();

        BookCollection col = store.loadBookCollection(collection, loadingErrors);

        if (col == null) {
            return;
        }

        report.println(collection);
        store.check(col, checkBits, errors, warnings);

        if (!errors.isEmpty()) {
            reportError("Errors:", errors);
            errors.clear();
        }
        if (!warnings.isEmpty()) {
            reportError("Warnings:", warnings);
            warnings.clear();
        }

        for (String bookName : store.listBooks(collection)) {
            Book book = store.loadBook(collection, bookName, loadingErrors);
            if (book == null) {
                report.println("Failed to read book. [" + collection + ":" + bookName + "]");
                continue;
            }
            report.println("\n" + bookName);
            store.check(col, book, checkBits, errors, warnings);

            if (!errors.isEmpty()) {
                reportError("Errors: ", errors);
                errors.clear();
            }
            if (!warnings.isEmpty()) {
                reportError("Warnings: ", warnings);
                warnings.clear();
            }
        }
    }

}
