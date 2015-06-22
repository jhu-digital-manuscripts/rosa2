package rosa.search.core;

import java.io.IOException;

import rosa.archive.core.Store;
import rosa.search.model.Query;
import rosa.search.model.SearchOptions;
import rosa.search.model.SearchResult;

/**
 * Service to execute index book collections and execute queries against that
 * content.
 * 
 * Implementations must be MT safe.
 */
interface SearchService {
    /**
     * Execute a search against the index. The search options determine how many
     * matches and what matches in the list of total results will be returned.
     * The resume token in the result and be used to efficiently retrieve more
     * matches.
     * 
     * @param query
     * @param opts
     * @return result
     * @throws IOException
     */
    SearchResult search(Query query, SearchOptions opts) throws IOException;

    /**
     * Index the collection indicated. Existing content in the index with same
     * identifier will be replaced.
     * 
     * @param store
     * @param collection_id
     * @throws IOException
     */
    void update(Store store, String collection_id) throws IOException;

    /**
     * Delete all indexed content.
     * 
     * @throws IOException
     */
    void clear() throws IOException;

    /**
     * Stop and cleanup the service.
     */
    void shutdown();
}
