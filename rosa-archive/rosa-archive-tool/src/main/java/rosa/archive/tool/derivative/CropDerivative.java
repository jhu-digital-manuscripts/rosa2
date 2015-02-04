package rosa.archive.tool.derivative;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import rosa.archive.core.Store;

public class CropDerivative extends BookDerivative {

    public CropDerivative(PrintStream report, Store store) {
        this(null, null, report, store);
    }

    public CropDerivative(String collection, PrintStream report, Store store) {
        this(collection, null, report, store);
    }

    public CropDerivative(String collection, String book, PrintStream report, Store store) {
        super(collection, book, report, store);
    }

    public boolean cropImages(boolean force) throws IOException {
        boolean result = true;

        if (collection == null) {
            for (String col : store.listBookCollections()) {
                for (String b : store.listBooks(col)) {
                    result = result && cropForBook(b, force);
                }
            }
        } else if (book == null) {
            for (String b : store.listBooks(collection)) {
                result = result && cropForBook(b, force);
            }
        } else {
            result = cropForBook(book, force);
        }

        return result;
    }

    private boolean cropForBook(String book, boolean force) throws IOException {
        List<String> errors = new ArrayList<>();

        store.cropImages(collection, book, force, errors);
        store.generateAndWriteCropList(collection, book, force, errors);

        if (!errors.isEmpty()) {
            reportError("Errors:", errors);
        }

        return errors.isEmpty();
    }

}