package rosa.search.tool;

import rosa.archive.core.Store;
import rosa.search.core.LuceneMapper;
import rosa.search.core.LuceneSearchService;
import rosa.search.core.SearchService;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;

/**
 * Create a search index based on current archive data.
 *
 * This class is designed to be used during the build process in order to create
 * the search index and have it available in the rosa website packages.
 */
public class SearchIndexDerivative extends Derivative {

    private final LuceneMapper mapper;

    public SearchIndexDerivative(String collectionName, Store archiveStore, Path targetPath, LuceneMapper mapper,
                                 PrintStream report) {
        super(collectionName, archiveStore, targetPath, report);
        this.mapper = mapper;
    }

    @Override
    public void update() {
        try {
            SearchService service = new LuceneSearchService(targetPath(), mapper);
            service.update(archiveStore(), collectionName());
        } catch (IOException e) {
            report().println("Could not create search index.");
            e.printStackTrace(report());
        } catch (NullPointerException e) {
            report().println("Archive not found, or collection not found.");
            e.printStackTrace(report());
        }
    }

}
