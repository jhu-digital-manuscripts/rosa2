package rosa.search.tool;

import rosa.archive.core.Store;
import rosa.archive.model.BookCollection;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public abstract class Derivative {
    private final PrintStream report;
    private final Store archiveStore;
    private final String collectionName;

    public Derivative(String collectionName, Store archiveStore, PrintStream report) {
        this.report = report;
        this.archiveStore = archiveStore;
        this.collectionName = collectionName;
    }

    public PrintStream report() {
        return report;
    }

    public Store archiveStore() {
        return archiveStore;
    }

    public String collectionName() {
        return collectionName;
    }

    protected BookCollection loadBookCollection() throws IOException {
        List<String> errors = new ArrayList<>();
        BookCollection collection = archiveStore.loadBookCollection(collectionName, errors);
        printErrors(errors);
        return collection;
    }

    protected void printErrors(List<String> errors) {
        if (!errors.isEmpty()) {
            report.println("Errors loading collection:");
            for (String er : errors) {
                report.println("  - " + er);
            }
        }
    }

    public abstract void update();
}
