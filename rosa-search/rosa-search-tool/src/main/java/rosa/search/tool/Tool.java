package rosa.search.tool;

import rosa.archive.core.Store;
import rosa.search.core.SearchService;

import java.io.PrintStream;

/**
 * This tool is designed to be used only during the project build process
 * to generate website resources.
 */
public class Tool {

    private final SearchService searchService;
    private final Store store;
    private final PrintStream report;

    public Tool(Store store, SearchService searchService, PrintStream report) {
        this.store = store;
        this.searchService = searchService;
        this.report = report;
    }

    private void indexCollection(String collection) {
        SearchIndexDerivative derivative =
                new SearchIndexDerivative(collection, store, searchService, report);
        derivative.update();
    }

    public void process(String[] args) {
        if (args.length != 1 && args.length != 0) {
            report.println("Must have zero or one arguments. Usage: <tool> <optional_collectionName>");
            return;
        }

        if (args.length == 0) {
            indexCollection(null);
        } else {
            indexCollection(args[0]);
        }
    }

}
