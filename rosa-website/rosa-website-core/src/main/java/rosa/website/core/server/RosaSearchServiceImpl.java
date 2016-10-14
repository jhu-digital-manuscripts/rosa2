package rosa.website.core.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import rosa.search.core.SearchService;
import rosa.search.model.Query;
import rosa.search.model.SearchOptions;
import rosa.search.model.SearchResult;
import rosa.website.core.client.RosaSearchService;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class RosaSearchServiceImpl extends ContextRemoteServiceServlet implements RosaSearchService {
    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(RosaSearchServiceImpl.class.toString());

    private StoreAccessLayer storeAccess;
    private SearchService searchService;

    private String collection;

    /** No-arg constructor for GWT */
    public RosaSearchServiceImpl() {}

    @Inject
    public RosaSearchServiceImpl(StoreAccessLayer storeAccess, SearchService searchService,
                                  @Named("collection.name") String collection) {
        this.storeAccess = storeAccess;
        this.searchService = searchService;
        this.collection = collection;
    }

    @Override
    public void init() {
        log.info("Initializing search service.");
        // Initialize search service:
        //   - Load search index from known location, if possible
        //   - Create search index if no index exists
        // Above done when the search service is instantiated!

        //   - Reindex books if changes are found?
        try {
            if (searchService.isEmpty()) {
                log.info("Search index empty, creating new index.");
                update();
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Could not initialize search service. ", e);
        }
    }

    @Override
    public void destroy() {
        if (searchService != null) {
            log.info("Shutting down search service.");
            searchService.shutdown();
        }
    }

    @Override
    public SearchResult search(Query query, SearchOptions options) throws IOException {
        if (query != null) {
            try {
                SearchResult result = searchService.search(query, options);

                return result;
            } catch (Exception e) {
                log.log(Level.SEVERE, "Error performing search.", e);

            }
        }

        return new SearchResult();
    }

    private void update() {
        if (!storeAccess.hasCollection(collection)) {
            log.severe("Collection not found in archive. [" + collection + "]");
            return;
        }

        try {
            log.info("Updating search index for collection [" + collection + "]");
            searchService.update(storeAccess.store(), collection);
            log.info("Done updating search index. [" + collection + "]");
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to update search index for collection. [" + collection + "]", e);
        }
    }
}
