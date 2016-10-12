package rosa.search.tool;

import rosa.archive.core.Store;
import rosa.search.core.LuceneMapper;

import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This tool is designed to be used only during the project build process
 * to generate website resources.
 */
public class Tool {

    private final LuceneMapper mapper;
    private final Store store;
    private final PrintStream report;

    public Tool(Store store, LuceneMapper mapper, PrintStream report) {
        this.store = store;
        this.mapper = mapper;
        this.report = report;
    }

    private void indexCollection(String collection, Path targetIndexPath) {
        SearchIndexDerivative derivative =
                new SearchIndexDerivative(collection, store, targetIndexPath, mapper, report);
        derivative.update();
    }

    public void process(String[] args) {
        if (args.length != 2) {
            report.println("Must provide two arguments. Usage: <tool> <collectionName> <indexPath>");
            return;
        }

        String collection = args[0];
        Path indexPath = Paths.get(args[1]);

        indexCollection(collection, indexPath);
    }

}
