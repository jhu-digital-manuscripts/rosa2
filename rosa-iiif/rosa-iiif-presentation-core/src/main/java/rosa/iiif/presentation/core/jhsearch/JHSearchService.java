package rosa.iiif.presentation.core.jhsearch;

import java.io.IOException;
import java.io.OutputStream;

import rosa.archive.core.Store;
import rosa.iiif.presentation.model.PresentationRequest;

public interface JHSearchService {
    public static String PROFILE_URI = "http://manuscriptlib.org/jhiff/search/profile";
    public static String CONTEXT_URI = "http://manuscriptlib.org/jhiff/search/context.json";
    public static String NAMESPACE_URI = "http://manuscriptlib.org/search/";
    public static String RESOURCE_PATH = "/jhsearch";
    public static String INFO_RESOURCE_PATH = "/jhsearch/info.json";
    public static String QUERY_PARAM = "q";
    public static String OFFSET_PARAM = "o";
    public static String RESUME_PARAM = "r";
    public static String MAX_MATCHES_PARAM = "m";
    
    /**
     * Perform a search on a presentation object, writing the result to an output stream.
     * May throw IllegalArgumentException to indicate a query or other parameter cannot be understood.
     * 
     * @param req
     * @param query
     * @param offset
     * @param max
     * @param os
     * @throws IOException
     * @throws IllegalArgumentException
     */
    void handle_request(PresentationRequest req, String query, int offset, int max, OutputStream os) throws IOException, IllegalArgumentException;

    void handle_request(PresentationRequest req, String query, String resume, int max, OutputStream os)
            throws IOException, IllegalArgumentException;

    default void handle_request(PresentationRequest req, String query, int offset, OutputStream os) throws IOException {
        handle_request(req, query, offset, -1, os);
    }

    default void handle_request(PresentationRequest req, String query, String resume, OutputStream os)
            throws IOException {
        handle_request(req, query, resume, -1, os);
    }
    
    /**
     * Respond to an information request about the search service available on a a presentation object.
     * 
     * @param req
     * @param os
     * @throws IOException
     */
    void handle_info_request(PresentationRequest req, OutputStream os) throws IOException;

    /**
     * Update the index to reflect the store.
     * 
     * @param store
     * @throws IOException
     */
    void update(Store store) throws IOException;

    /**
     * Must be called when the service is done being used.
     * 
     * @throws IOException
     */
    void shutdown() throws IOException;
}
