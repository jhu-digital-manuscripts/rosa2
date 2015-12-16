package rosa.iiif.search.endpoint;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import rosa.iiif.presentation.core.IIIFRequestParser;
import rosa.iiif.presentation.model.PresentationRequest;
import rosa.iiif.search.core.IIIFSearchJsonldSerializer;
import rosa.iiif.search.core.IIIFSearchService;
import rosa.iiif.search.model.IIIFSearchRequest;
import rosa.iiif.search.model.IIIFSearchResult;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class IIIFSearchServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(IIIFSearchServlet.class.toString());
    private static final long serialVersionUID = 1L;

    private static final String JSON_MIME_TYPE = "application/json";
    private static final String JSON_LD_MIME_TYPE = "application/ld+json";

    private static final String PARAM_Q = "q";
    private static final String PARAM_MOTIVATION = "motivation";
    private static final String PARAM_PAGE = "page";
//    private static final String PARAM_DATE = "date";
//    private static final String PARAM_USER = "user";
//    private static final String PARAM_BOX = "box";

    private IIIFSearchService searchService;
    private IIIFRequestParser requestParser;
    private IIIFSearchJsonldSerializer serializer;
    private String[] ignoredParams;

    @Inject
    public IIIFSearchServlet(IIIFSearchService searchService, IIIFSearchJsonldSerializer serializer,
                             @Named("ignored.parameters") String[] ignoredParams,
                             IIIFRequestParser requestParser) {
        logger.info("Creating IIIF search servlet. ");
        this.searchService = searchService;
        this.serializer = serializer;
        this.ignoredParams = ignoredParams;
        this.requestParser = requestParser;
    }

    @Override
    public void init() throws ServletException {
        try {
            logger.info("Updating IIIF Search Service index.");
            searchService.update();
            logger.info("Done updating IIIF Search Service index.");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to update index for IIIF search service.", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String query = req.getParameter(PARAM_Q);
        String motivation = req.getParameter(PARAM_MOTIVATION);
        int page = -1;
        try {
            String p = req.getParameter(PARAM_PAGE);
            if (p != null && !p.isEmpty()) {
                page = Integer.parseInt(req.getParameter(PARAM_PAGE));
            }
        } catch (NumberFormatException e) {
            logger.warning("'page' parameter not a number. [" + req.getParameter(PARAM_PAGE) + "]");
        }

        resp.setHeader("Access-Control-Allow-Origin", "*");

        if (want_json_ld_mime_type(req)) {
            resp.setContentType(JSON_LD_MIME_TYPE);
        } else {
            resp.setContentType(JSON_MIME_TYPE);
            resp.addHeader(
                    "Link",
                    "<http://iiif.io/api/presentation/2/context.json>;" +
                            "rel=\"http://www.w3.org/ns/json-ld#context\";type=\"application/ld+json\"," +
                            "<http://iiif.io/api/search/0/context.json>;" +
                            "rel=\"http://www.w3.org/ns/json-ld#context\";type=\"application/ld+json\"");
        }

        // Get IIIF object ID from request path
        PresentationRequest r =
                requestParser.parsePresentationRequest(get_raw_path(req).replace("/search", ""));

        if (r == null || r.getType() == null) {
            // No or invalid ID was found in URI
            resp.sendError(HttpURLConnection.HTTP_NOT_FOUND, "No such object: " + req.getRequestURL().toString());
        } else {
            // Good ID found, do search
            IIIFSearchRequest searchRequest;
            if (page < 0) {
                searchRequest = new IIIFSearchRequest(r, query, motivation);
            } else {
                searchRequest = new IIIFSearchRequest(r, query, motivation, page);
            }

            IIIFSearchResult result = searchService.search(searchRequest);
            result.setIgnored(ignoredParams);
            serializer.write(result, resp.getOutputStream());
        }

        resp.flushBuffer();
    }

// ----- Some methods taken from rosa.iiif.presentation.endpoint.IIIFServlet -----

    private boolean want_json_ld_mime_type(HttpServletRequest req) {
        String accept = req.getHeader("Accept");
        return accept != null && accept.contains(JSON_LD_MIME_TYPE);
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
}
