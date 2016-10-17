package rosa.search.tool;

import rosa.archive.core.Store;
import rosa.search.core.SearchService;

import java.io.IOException;
import java.io.PrintStream;

/**
 * Create a search index based on current archive data.
 *
 * This class is designed to be used during the build process in order to create
 * the search index and have it available in the rosa website packages.
 */
public class SearchIndexDerivative extends Derivative {

    private final SearchService searchService;

    public SearchIndexDerivative(String collectionName, Store archiveStore, SearchService searchService, PrintStream report) {
        super(collectionName, archiveStore, report);
        this.searchService = searchService;
    }

    @Override
    public void update() {
        try {
            searchService.update(archiveStore(), collectionName());
        } catch (IOException e) {
            report().println("Could not create search index.");
            e.printStackTrace(report());
        } catch (NullPointerException e) {
            report().println("Archive not found, or collection not found.");
            e.printStackTrace(report());
        }
    }

}
