package rosa.website.tool;

import rosa.archive.core.Store;

import java.io.PrintStream;
import java.nio.file.Path;

/**
 * Create a search index based on current archive data.
 *
 * This class is designed to be used during the build process in order to create
 * the search index and have it available in the rosa website packages.
 */
public class WebsiteSearchIndexDerivative extends Derivative {

    public WebsiteSearchIndexDerivative(String collectionName, Store archiveStore, Path targetPath,
                                        PrintStream report) {
        super(collectionName, archiveStore, targetPath, report);
    }

    @Override
    public void update() {

    }



}
