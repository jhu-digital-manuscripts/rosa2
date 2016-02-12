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
    public static String QUERY_PARAM = "q";
    public static String OFFSET_PARAM = "o";
    public static String RESUME_PARAM = "r";
    public static String MAX_MATCHES_PARAM = "m";
    
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

    void update(Store store) throws IOException;

    void shutdown() throws IOException;
}
