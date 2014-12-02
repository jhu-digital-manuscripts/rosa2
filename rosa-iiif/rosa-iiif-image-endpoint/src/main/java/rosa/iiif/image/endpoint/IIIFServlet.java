package rosa.iiif.image.endpoint;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import rosa.iiif.image.core.ImageServer;
import rosa.iiif.image.model.ImageInfo;
import rosa.iiif.image.model.ImageRequest;
import rosa.iiif.image.model.InfoFormat;
import rosa.iiif.image.model.InfoRequest;
import rosa.iiif.image.model.RequestType;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

// TODO check error code handling

/**
 * Implement the IIIF Image API version 2.0, http://iiif.io/api/image/2.0/
 */
@Singleton
public class IIIFServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final ImageServer server;
    private final IIIFRequestParser parser;
    private final IIIFResponseSerializer serializer;
    private final Map<String, String> image_id_aliases; // alias -> image id

    @Inject
    public IIIFServlet(ImageServer server, @Named("image.aliases") Map<String, String> image_id_aliases) {
        this.server = server;
        this.serializer = new IIIFResponseSerializer();
        this.parser = new IIIFRequestParser();
        this.image_id_aliases = image_id_aliases;
    }

    private void report_error(HttpServletResponse resp, int code, String message) throws ServletException, IOException {
        resp.setStatus(code);
        resp.setContentType("text/plain");

        resp.getWriter().print("Error: " + message);
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.addHeader("Link", "<" + server.getCompliance().getUri() + ">;rel=\"profile\"");

        // Hack to get raw path;

        String context = req.getContextPath();
        StringBuffer sb = req.getRequestURL();
        int i = sb.indexOf(context);

        if (i == -1) {
            throw new ServletException("Cannot find " + context + " in " + sb);
        }

        String path = sb.substring(i + context.length());

        RequestType type = parser.determineRequestType(path);

        try {
            if (type == RequestType.INFO) {
                InfoRequest inforeq = parser.parseImageInfoRequest(path);

                String alias = image_id_aliases.get(inforeq.getImageId());

                if (alias != null) {
                    inforeq.setImageId(alias);
                }

                ImageInfo info = server.lookupImage(inforeq.getImageId());

                if (info == null) {
                    report_error(resp, 404, "image not found: " + inforeq.getImageId());
                } else {
                    OutputStream os = resp.getOutputStream();
                    resp.setContentType(inforeq.getFormat().getMimeType());

                    try {
                        if (inforeq.getFormat() == InfoFormat.JSON) {
                            serializer.writeJsonLd(info, os);
                        } else {
                            report_error(resp, 415, "no such info format");
                        }
                    } catch (JSONException e) {
                        throw new ServletException(e);
                    }
                }

            } else if (type == RequestType.IMAGE) {
                ImageRequest imgreq = parser.parseImageRequest(path);

                String alias = image_id_aliases.get(imgreq.getImageId());

                if (alias != null) {
                    imgreq.setImageId(alias);
                }

                String imgurl = server.constructURL(imgreq);

                if (imgurl == null) {
                    report_error(resp, 404, "image not found: " + imgreq.getImageId());
                } else {
                    forward(imgurl, resp);
                }
            } else {
                report_error(resp, 400, "malformed request");
            }
        } catch (IIIFException e) {
            String param = e.getParameter() == null ? "unknown" : e.getParameter();
            int code = 400;

            if (param.equals("identifier")) {
                code = 404;
            } else if (param.equals("format")) {
                code = 415;
            }

            report_error(resp, code, e.getMessage());
        }

        resp.flushBuffer();
    }

    private void set_header_if_exists(String header, URLConnection from, HttpServletResponse to) {
        String value = from.getHeaderField(header);

        if (value != null) {
            to.setHeader(header, value);
        }
    }

    private void forward(String url, HttpServletResponse resp) throws IOException {
        URLConnection con = new URL(url).openConnection();
        con.connect();

        // TODO investigate what headers to copy

        set_header_if_exists("Last-Modified", con, resp);
        set_header_if_exists("Content-Type", con, resp);
        set_header_if_exists("Content-Length", con, resp);

        InputStream is = con.getInputStream();
        OutputStream os = resp.getOutputStream();

        IOUtils.copy(is, os);
    }
}
