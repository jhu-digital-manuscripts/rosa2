package rosa.iiif.presentation.endpoint;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import rosa.archive.core.Store;
import rosa.iiif.presentation.core.IIIFPresentationRequestParser;
import rosa.iiif.presentation.core.IIIFPresentationService;
import rosa.iiif.presentation.core.search.IIIFSearchJsonldSerializer;
import rosa.iiif.presentation.core.search.IIIFSearchService;
import rosa.iiif.presentation.model.PresentationRequest;
import rosa.iiif.presentation.model.search.IIIFSearchRequest;
import rosa.iiif.presentation.model.search.IIIFSearchResult;

/**
 * Implement the IIIF Presentation API version 2.0, http://iiif.io/api/presentation/2.0/
 */
@Singleton
public class IIIFPresentationServlet extends HttpServlet {   
    private static final Logger logger = Logger.getLogger("");
    private static final long serialVersionUID = 1L;
    private static final String JSON_MIME_TYPE = "application/json";
    private static final String JSON_LD_MIME_TYPE = "application/ld+json";
    private static final String SEARCH_SUFFIX = "/search";
    private static final String PARAM_Q = "q";
    private static final String PARAM_MOTIVATION = "motivation";
    private static final String PARAM_PAGE = "page";
            
    private final IIIFPresentationService service;
    private final IIIFPresentationRequestParser parser;
    private final IIIFSearchService searchService;
    private final IIIFSearchJsonldSerializer searchSerializer;
    private final Store store;
    
    /**
     * Create a servlet for the IIIF presentation layer.
     * @param service a IIIFService that knows how to handle requests
     */
    @Inject
    public IIIFPresentationServlet(IIIFPresentationService service, IIIFSearchService searchService, Store store) {
        this.service = service;
        this.searchService = searchService;
        this.parser = new IIIFPresentationRequestParser();
        this.searchSerializer = new IIIFSearchJsonldSerializer();
        this.store = store;
    }
    

    @Override
    public void init() throws ServletException {
        try {
            logger.info("Updating IIIF Presentation Search Service index.");
            searchService.update(store);    
            logger.info("Done updating IIIF Presentation Search Service index.");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to update index for IIIF Presentation Search Service.", e);
        }
    }

    @Override
    public void destroy() {
        try {
            searchService.shutdown();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to shutdown IIIF Presentation Search Service", e);
        }
        
        super.destroy();
    }

    private String get_raw_path(HttpServletRequest req) throws ServletException {
        String context = req.getContextPath();
        StringBuffer sb = req.getRequestURL();
        int i = sb.indexOf(context);

        if (i == -1) {
            throw new ServletException("Cannot find " + context + " in " + sb);
        }

        return sb.substring(i + context.length());
    }

    private boolean want_json_ld_mime_type(HttpServletRequest req) {
        String accept = req.getHeader("Accept");

        if (accept != null && accept.contains(JSON_LD_MIME_TYPE)) {
            return true;
        }

        return false;
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Access-Control-Allow-Origin", "*");

        if (want_json_ld_mime_type(req)) {
            resp.setContentType(JSON_LD_MIME_TYPE);
        } else {
            resp.setContentType(JSON_MIME_TYPE);
            resp.addHeader(
                    "Link",
                    "<http://iiif.io/api/presentation/2/context.json>;rel=\"http://www.w3.org/ns/json-ld#context\";type=\"application/ld+json\"");
        }
        
        
        String raw_path = get_raw_path(req);
        
        if (raw_path.endsWith(SEARCH_SUFFIX)) {
            // Search request
            
            raw_path = raw_path.substring(0, raw_path.length() - SEARCH_SUFFIX.length());
            
            String query = req.getParameter(PARAM_Q);
            String motivation = req.getParameter(PARAM_MOTIVATION);
            int page = -1;
            
            try {
                String p = req.getParameter(PARAM_PAGE);
                
                if (p != null && !p.isEmpty()) {
                    page = Integer.parseInt(p);
                }
            } catch (NumberFormatException e) {
            }
            
            PresentationRequest presreq = parser.parsePresentationRequest(get_raw_path(req));
            
            if (presreq == null) {
                resp.sendError(HttpURLConnection.HTTP_NOT_FOUND, "No such object: " + req.getRequestURL().toString());
            } else {
                IIIFSearchRequest searchRequest;
                
                if (page < 0) {
                    searchRequest = new IIIFSearchRequest(presreq, query, motivation);
                } else {
                    searchRequest = new IIIFSearchRequest(presreq, query, motivation, page);
                }

                IIIFSearchResult result = searchService.search(searchRequest);
                searchSerializer.write(result, resp.getOutputStream());
            }
            
        } else {
            // Check if request follows recommended URI pattern
            
            PresentationRequest presreq = parser.parsePresentationRequest(get_raw_path(req));
            String uri = req.getRequestURL().toString();

            OutputStream os = resp.getOutputStream();

            if (presreq == null) {
                if (!service.handle_request(uri, os)) {
                    resp.sendError(HttpURLConnection.HTTP_NOT_FOUND, "No such object: " + uri);
                }
            } else {
                if (!service.handle_request(presreq, os)) {
                    resp.sendError(HttpURLConnection.HTTP_NOT_FOUND, "No such object: " + uri);
                }
            }
        }

        resp.flushBuffer();
    }
}
