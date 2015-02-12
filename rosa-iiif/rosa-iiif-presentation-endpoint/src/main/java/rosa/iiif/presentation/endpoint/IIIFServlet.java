package rosa.iiif.presentation.endpoint;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import rosa.iiif.presentation.core.IIIFRequestParser;
import rosa.iiif.presentation.core.IIIFService;
import rosa.iiif.presentation.model.PresentationRequest;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Implement the IIIF Presentation API version 2.0, http://iiif.io/api/presentation/2.0/
 */
@Singleton
public class IIIFServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String JSON_MIME_TYPE = "application/json";
    private static final String JSON_LD_MIME_TYPE = "application/ld+json";

    private final IIIFService service;
    private final IIIFRequestParser parser;

    /**
     * Create a IIIF servlet for the IIIF presentation layer.
     * @param service a IIIFService that knows how to handle requests
     */
    @Inject
    public IIIFServlet(IIIFService service) {
        this.service = service;
        this.parser = new IIIFRequestParser();
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
        OutputStream os = resp.getOutputStream();

        // Check if request follows recommended URI pattern

        PresentationRequest presreq = parser.parsePresentationRequest(get_raw_path(req));
        String uri = req.getRequestURL().toString();

        resp.setHeader("Access-Control-Allow-Origin", "*");

        if (want_json_ld_mime_type(req)) {
            resp.setContentType(JSON_LD_MIME_TYPE);
        } else {
            resp.setContentType(JSON_MIME_TYPE);
            resp.addHeader(
                    "Link",
                    "<http://iiif.io/api/presentation/2/context.json>;rel=\"http://www.w3.org/ns/json-ld#context\";type=\"application/ld+json\"");
        }

        if (presreq == null) {
            if (!service.handle_request(uri, os)) {
                resp.sendError(HttpURLConnection.HTTP_NOT_FOUND, "No such object: " + uri);
            }
        } else {
            if (!service.handle_request(presreq, os)) {
                resp.sendError(HttpURLConnection.HTTP_NOT_FOUND, "No such object: " + uri);
            }
        }

        resp.flushBuffer();
    }
}
