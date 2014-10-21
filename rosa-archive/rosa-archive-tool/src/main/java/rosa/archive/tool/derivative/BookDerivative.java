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

    @Override
    public void check(boolean checkBits) throws IOException {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> loadingErrors = new ArrayList<>();

        BookCollection col = store.loadBookCollection(collection, loadingErrors);
        Book b = col == null ? null : store.loadBook(collection, book, loadingErrors);
        if (b != null) {
            store.check(col, b, checkBits, errors, warnings);
        } else {
            report.println("Failed to read book. [" + collection + ":" + book + "]");
        }

        if (!errors.isEmpty()) {
            reportError("Errors: ", errors);
        }
        if (!warnings.isEmpty()) {
            reportError("Warnings: ", warnings);
        }
    }

}
