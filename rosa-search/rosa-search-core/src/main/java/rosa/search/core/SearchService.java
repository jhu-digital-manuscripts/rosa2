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
public interface SearchService {
    /**
     * Execute a search against the index. The search options determine how many
     * matches and what matches in the list of total results will be returned.
     * The resume token in the result and be used to efficiently retrieve more
     * matches.
     * 
     * @param query rosa query object
     * @param opts search options
     * @return result
     * @throws IOException if search service is unavailable
     */
    SearchResult search(Query query, SearchOptions opts) throws IOException;
    
    /**
     * Index the collection indicated. Existing content in the index with same
     * identifier will be replaced.
     * 
     * @param store archive store
     * @param collection_id ID of collection to index
     * @throws IOException if archive or search service is unavailable
     */
    void update(Store store, String collection_id) throws IOException;

    /**
     * Delete all indexed content.
     * 
     * @throws IOException if service is unavailable
     */
    void clear() throws IOException;

    /**
     * @return Whether or not the index is empty of content.
     * @throws IOException  if service is unavailable
     */
    boolean isEmpty() throws IOException;
    
    /**
     * Stop and cleanup the service.
     */
    void shutdown();
}
