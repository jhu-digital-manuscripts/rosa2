package rosa.website.core.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import rosa.archive.core.Store;
import rosa.search.core.SearchService;
import rosa.search.model.Query;
import rosa.search.model.SearchOptions;
import rosa.search.model.SearchResult;
import rosa.website.search.client.RosaSearchService;

import java.util.logging.Logger;

@Singleton
public class RosaSearchServiceImpl extends RemoteServiceServlet implements RosaSearchService {
    private static final Logger log = Logger.getLogger(RosaSearchServiceImpl.class.toString());

    private Store archiveStore;
    private SearchService searchService;

    @Inject
    private RosaSearchServiceImpl(StoreProvider storeProvider, @Named("archive.path") String archivePath,
                                  SearchService searchService) {
        this.archiveStore = storeProvider.getStore(archivePath);
        this.searchService = searchService;
    }

    @Override
    public void init() {
        log.info("Initializing search service.");
        // Initialize search service:
        //   - Load search index from known location, if possible
        //   - Create search index if no index exists
        //   - Reindex books if changes are found? (Run this as separate thread)
    }

    @Override
    public SearchResult search(Query query, SearchOptions options) {
        return null;
    }
}
