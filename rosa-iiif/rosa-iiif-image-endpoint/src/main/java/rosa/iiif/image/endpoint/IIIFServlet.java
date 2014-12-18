package rosa.iiif.image.endpoint;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;

import rosa.iiif.image.core.IIIFException;
import rosa.iiif.image.core.IIIFRequestParser;
import rosa.iiif.image.core.IIIFResponseSerializer;
import rosa.iiif.image.core.IIIFService;
import rosa.iiif.image.model.ImageInfo;
import rosa.iiif.image.model.ImageRequest;
import rosa.iiif.image.model.InfoFormat;
import rosa.iiif.image.model.InfoRequest;
import rosa.iiif.image.model.RequestType;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * Implement the IIIF Image API version 2.0, http://iiif.io/api/image/2.0/
 */
@Singleton
public class IIIFServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final IIIFService service;
    private final IIIFRequestParser parser;
    private final IIIFResponseSerializer serializer;
    private final Map<String, String> image_id_aliases; // alias -> image id

    @Inject
    public IIIFServlet(IIIFService service, @Named("image.aliases") Map<String, String> image_id_aliases) {
        this.service = service;
        this.serializer = new IIIFResponseSerializer();
        this.parser = new IIIFRequestParser();
        this.image_id_aliases = image_id_aliases;
    }

    private void report_error(HttpServletResponse resp, IIIFException e) throws IOException {
        int code = e.getHttpCode();

        resp.setStatus(code);
        resp.setContentType("text/plain");

        String overview;

        if (code == HttpURLConnection.HTTP_BAD_REQUEST) {
            overview = "Request not understood.";
        } else if (code == HttpURLConnection.HTTP_INTERNAL_ERROR) {
            overview = "Internal error handling request.";
        } else if (code == HttpURLConnection.HTTP_NOT_IMPLEMENTED) {
            overview = "Requested functionality not available.";
        } else {
            overview = "Error handling request.";
        }

        String message = "";

        if (e.getMessage() != null) {
            message = e.getMessage();
        }

        PrintStream out = new PrintStream(resp.getOutputStream());

        out.println(overview);
        out.println(message);

        // Stack trace only on internal errors.

        if (code == HttpURLConnection.HTTP_INTERNAL_ERROR) {
            out.println("Stack trace:");
            e.printStackTrace(out);
        }

        out.flush();
    }

    private String get_raw_path(HttpServletRequest req) throws IIIFException {
        String context = req.getContextPath();
        StringBuffer sb = req.getRequestURL();
        int i = sb.indexOf(context);

        if (i == -1) {
            throw new IIIFException("Cannot find " + context + " in " + sb, HttpURLConnection.HTTP_INTERNAL_ERROR);
        }

        return sb.substring(i + context.length());
    }

    private boolean want_json_ld_mime_type(HttpServletRequest req) {
        String accept = req.getHeader("Accept");

        if (accept != null && accept.contains(InfoFormat.JSON_LD.getMimeType())) {
            return true;
        }

        return false;
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.addHeader("Link", "<" + service.getCompliance().getUri() + ">;rel=\"profile\"");

        try {
            String path = get_raw_path(req);

            RequestType type = parser.determineRequestType(path);

            if (type == RequestType.INFO) {
                InfoRequest inforeq = parser.parseImageInfoRequest(path);

                String alias = image_id_aliases.get(inforeq.getImageId());

                if (alias != null) {
                    inforeq.setImageId(alias);
                }

                ImageInfo info = service.perform(inforeq);

                resp.setHeader("Access-Control-Allow-Origin", "*");

                OutputStream os = resp.getOutputStream();

                InfoFormat fmt = inforeq.getFormat();

                if (want_json_ld_mime_type(req)) {
                    fmt = InfoFormat.JSON_LD;
                }

                if (fmt == InfoFormat.JSON) {
                    resp.addHeader("Link",
                            "<http://iiif.io/api/image/2/context.json>;rel=\"http://www.w3.org/ns/json-ld#context\";type=\"application/ld+json\"");
                }

                resp.setContentType(fmt.getMimeType());

                String url = req.getRequestURL().toString();

                // Must set image uri to request url without /info.json
                info.setImageUri(url.substring(0, url.length() - "/info.json".length()));

                try {
                    serializer.writeJsonLd(info, os);
                } catch (JSONException e) {
                    throw new IIIFException("Error writing JSON-LD", e, HttpURLConnection.HTTP_INTERNAL_ERROR);
                }

            } else if (type == RequestType.OPERATION) {
                ImageRequest imgreq = parser.parseImageRequest(path);

                String alias = image_id_aliases.get(imgreq.getImageId());

                if (alias != null) {
                    imgreq.setImageId(alias);
                }

                perform_image_request(imgreq, resp);
            } else if (type == RequestType.IMAGE) {
                // Redirect to info request
                resp.sendRedirect("info.json");
            } else {
                throw new IIIFException("Request type not handled: " + type, HttpURLConnection.HTTP_INTERNAL_ERROR);
            }
        } catch (IIIFException e) {
            report_error(resp, e);
        }

        resp.flushBuffer();
    }

    private void set_header_if_exists(String header, URLConnection from, HttpServletResponse to) {
        String value = from.getHeaderField(header);

        if (value != null) {
            to.setHeader(header, value);
        }
    }

    // Either submit request to a URL or perform directly
    private void perform_image_request(ImageRequest req, HttpServletResponse resp) throws IOException, IIIFException {
        OutputStream os = resp.getOutputStream();

        if (service.supportsImageRequestURL()) {
            URLConnection con = new URL(service.performURL(req)).openConnection();
            con.connect();

            set_header_if_exists("Last-Modified", con, resp);
            set_header_if_exists("Content-Type", con, resp);
            set_header_if_exists("Content-Length", con, resp);

            try (InputStream is = con.getInputStream()) {
                IOUtils.copy(is, os);
            }
        } else {
            try (InputStream is = service.perform(req)) {
                resp.setContentType(req.getFormat().getMimeType());

                IOUtils.copy(is, os);
            }
        }
    }
}
