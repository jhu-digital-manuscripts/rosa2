package rosa.website.core.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.inject.Guice;
import com.google.inject.Injector;
import rosa.archive.core.ArchiveCoreModule;
import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.FSByteStreamGroup;
import rosa.archive.core.Store;
import rosa.archive.core.StoreImpl;
import rosa.archive.core.check.BookChecker;
import rosa.archive.core.check.BookCollectionChecker;
import rosa.archive.core.serialize.SerializerSet;
import rosa.search.model.Query;
import rosa.search.model.SearchOptions;
import rosa.search.model.SearchResult;
import rosa.website.core.client.RosaSearchService;

import java.util.logging.Logger;

public class RosaSearchServiceImpl extends RemoteServiceServlet implements RosaSearchService {
    private static final Logger log = Logger.getLogger(RosaSearchServiceImpl.class.toString());

    // TODO either use the ArchiveDataService directly, or otherwise share the Store instance
    private Store archiveStore;

    @Override
    public void init() {
        // TODO initialize search service:
        //   - Load search index from known location, if possible
        //   - Create search index if no index exists
        //   - Reindex books if changes are found? (Run this as separate thread)

        // -------------------------------------------------------------------------------------
        // ----- either use the ArchiveDataService directly, or otherwise share the Store instance
        log.info("Initializing RosaSearchService.");
        Injector injector = Guice.createInjector(new ArchiveCoreModule());

        String path = getServletContext().getInitParameter("archive-path");
        if (path == null || path.isEmpty()) {
            log.warning("'archive-path' not specified. Using default value [/mnt]");
            path = "/mnt";
        }

        ByteStreamGroup base = new FSByteStreamGroup(path);
        this.archiveStore = new StoreImpl(injector.getInstance(SerializerSet.class),
                injector.getInstance(BookChecker.class), injector.getInstance(BookCollectionChecker.class), base);
        log.info("Archive Store set.");
        // -------------------------------------------------------------------------------------


    }

    @Override
    public SearchResult search(Query query, SearchOptions options) {
        return null;
    }
}
