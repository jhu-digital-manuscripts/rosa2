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
import rosa.iiif.presentation.core.jhsearch.JHSearchService;
import rosa.iiif.presentation.model.PresentationRequest;

/**
 * Implement the IIIF Presentation API version 2.0,
 * http://iiif.io/api/presentation/2.0/
 */
@Singleton
public class IIIFPresentationServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger("");
    private static final long serialVersionUID = 1L;
    private static final String JSON_MIME_TYPE = "application/json";
    private static final String JSON_LD_MIME_TYPE = "application/ld+json";

    private final IIIFPresentationService service;
    private final IIIFPresentationRequestParser parser;
    private final JHSearchService searchService;
    private final Store store;

    /**
     * Create a servlet for the IIIF presentation layer.
     * 
     * @param service
     *            a IIIFService that knows how to handle requests
     */
    @Inject
    public IIIFPresentationServlet(IIIFPresentationService service, JHSearchService searchService, Store store) {
        this.service = service;
        this.searchService = searchService;
        this.parser = new IIIFPresentationRequestParser();
        this.store = store;
    }

    @Override
    public void init() throws ServletException {
        try {
            if (searchService.has_content()) {
                logger.info("Using existing IIIF Presentation Search Service index.");
            } else {
                logger.info("Updating IIIF Presentation Search Service index.");

                searchService.update(store);

                logger.info("Done updating IIIF Presentation Search Service index.");
            }
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

    private int get_int_param(HttpServletRequest req, String param, int default_val) {
        int result = default_val;

        try {
            String val = req.getParameter(param);

            if (val != null) {
                result = Integer.parseInt(val);
            }
        } catch (NumberFormatException e) {
        }

        return result;
    }

    // Provide a way to send a plain text error message.
    private void send_error(HttpServletResponse resp, int code, String message) throws IOException {
        resp.resetBuffer();
        resp.setStatus(code);
        resp.setContentType("text/plain");
        resp.getOutputStream().write(message.getBytes(resp.getCharacterEncoding()));
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setCharacterEncoding("utf-8");

        if (want_json_ld_mime_type(req)) {
            resp.setContentType(JSON_LD_MIME_TYPE);
        } else {
            resp.setContentType(JSON_MIME_TYPE);
            resp.addHeader("Link",
                    "<http://iiif.io/api/presentation/2/context.json>;rel=\"http://www.w3.org/ns/json-ld#context\";type=\"application/ld+json\"");
        }

        OutputStream os = resp.getOutputStream();
        String raw_path = get_raw_path(req);

        // Request is either for a IIF Presentation object, a search within that object, or search info

        if (raw_path.endsWith(JHSearchService.INFO_RESOURCE_PATH)) {
            // Search info request

            raw_path = raw_path.substring(0, raw_path.length() - JHSearchService.INFO_RESOURCE_PATH.length());

            // Must follow recommended URI pattern

            PresentationRequest presreq = parser.parsePresentationRequest(raw_path);

            if (presreq == null) {
                send_error(resp, HttpURLConnection.HTTP_NOT_FOUND, "No such object: " + req.getRequestURL());
            }

            searchService.handle_info_request(presreq, os);
        } else if (raw_path.endsWith(JHSearchService.RESOURCE_PATH)) {
            // Search request

            raw_path = raw_path.substring(0, raw_path.length() - JHSearchService.RESOURCE_PATH.length());

            String query = req.getParameter(JHSearchService.QUERY_PARAM);
            int offset = get_int_param(req, JHSearchService.OFFSET_PARAM, 0);
            int max = get_int_param(req, JHSearchService.MAX_MATCHES_PARAM, -1);
            String sort_order = req.getParameter(JHSearchService.SORT_ORDER_PARAM);
            String categories = req.getParameter(JHSearchService.CATEGORIES);

            // Must follow recommended URI pattern

            PresentationRequest presreq = parser.parsePresentationRequest(raw_path);

            if (presreq == null) {
                send_error(resp, HttpURLConnection.HTTP_NOT_FOUND, "No such object: " + req.getRequestURL());
            } else {
                try {
                    searchService.handle_request(presreq, query, offset, max, sort_order, categories, os);
                } catch (IllegalArgumentException e) {
                    send_error(resp, HttpURLConnection.HTTP_BAD_REQUEST, e.getMessage());
                }
            }
        } else {
            // Check if request follows recommended URI pattern

            PresentationRequest presreq = parser.parsePresentationRequest(raw_path);
            String uri = req.getRequestURL().toString();

            if (presreq == null) {
                if (!service.handle_request(uri, os)) {
                    send_error(resp, HttpURLConnection.HTTP_NOT_FOUND, "No such object: " + uri);
                }
            } else {
                if (!service.handle_request(presreq, os)) {
                    send_error(resp, HttpURLConnection.HTTP_NOT_FOUND, "No such object: " + uri);
                }
            }
        }

        resp.flushBuffer();
    }
}
