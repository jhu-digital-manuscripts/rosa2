package rosa.iiif.image.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.json.JSONException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import rosa.iiif.image.model.ImageInfo;
import rosa.iiif.image.model.ImageRequest;
import rosa.iiif.image.model.InfoFormat;
import rosa.iiif.image.model.InfoRequest;
import rosa.iiif.image.model.RequestType;

public class IIIFServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private ImageServer server;
    private IIIFParser parser;
    private IIIFSerializer serializer;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        String fsi_url = config.getInitParameter("fsi.url");

        if (fsi_url == null) {
            throw new ServletException(
                    "Required init parameter 'fsi.url' not set");
        }

        parser = new IIIFParser();
        serializer = new IIIFSerializer();
        server = new FSIServer(fsi_url);

        Enumeration<?> params = config.getInitParameterNames();

        while (params.hasMoreElements()) {
            String param = (String) params.nextElement();

            if (param.startsWith("alias.")) {
                String alias = param.substring("alias.".length());
                parser.getImageAliases().put(alias,
                        config.getInitParameter(param));
            }
        }
    }

    private void report_error(HttpServletResponse resp, int code,
            String message, String param) throws ServletException, IOException {
        resp.setStatus(code);
        resp.setContentType("application/xml");

        DocumentBuilderFactory docFactory = DocumentBuilderFactory
                .newInstance();
        try {
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            String ns = "http://library.stanford.edu/iiif/image-api/ns/";
            Document doc = docBuilder.newDocument();
            Element root = doc.createElementNS(ns, "error");
            doc.appendChild(root);

            Element param_el = doc.createElementNS(ns, "parameter");
            root.appendChild(param_el);
            param_el.setTextContent(param);

            Element text_el = doc.createElementNS(ns, "text");
            root.appendChild(text_el);
            text_el.setTextContent(message);

            TransformerFactory transformerFactory = TransformerFactory
                    .newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);

            StreamResult result = new StreamResult(resp.getOutputStream());
            transformer.transform(source, result);
        } catch (TransformerException e) {
            throw new ServletException(e);
        } catch (ParserConfigurationException e) {
            throw new ServletException(e);
        }
    }

    private static void copy(InputStream in, OutputStream out)
            throws IOException {
        byte[] buf = new byte[16 * 1024];
        int n = 0;

        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.addHeader(
                "Link",
                "<http://library.stanford.edu/iiif/image-api/compliance.html#level0>;rel=\"profile\"");

        // Hack to get undecoded path;

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
                ImageInfo info = server.lookupImage(inforeq.getImageId());

                if (info == null) {
                    report_error(resp, 404, "not found", "identifier");
                } else {
                    OutputStream os = resp.getOutputStream();

                    try {
                        // TODO
                        if (inforeq.getFormat() == InfoFormat.JSON) {
                            String callback = req.getParameter("callback");

                            if (callback != null) {
                                os.write(callback.getBytes("UTF-8"));
                                os.write('(');
                            }

                            serializer.toJSON(info, os);
                            if (callback != null) {
                                os.write(')');
                            }
                        } else {
                            report_error(resp, 415, "no such info format",
                                    "format");
                        }
                    } catch (JSONException e) {
                        throw new ServletException(e);
                    }
                }

            } else if (type == RequestType.IMAGE) {
                ImageRequest imgreq = parser.parseImageRequest(path);

                String imgurl = server.constructURL(imgreq);

                if (imgurl == null) {
                    report_error(resp, 404, "not found", "identifier");
                } else {
                    forward(imgurl, resp);
                }
            } else {
                report_error(resp, 400, "malformed request", "unknown");
            }
        } catch (IIIFException e) {
            String param = e.getParameter() == null ? "unknown" : e
                    .getParameter();
            int code = 400;

            if (param.equals("identifier")) {
                code = 404;
            } else if (param.equals("format")) {
                code = 415;
            }

            report_error(resp, code, e.getMessage(), param);
        }

        resp.flushBuffer();
    }

    private void set_header_if_exists(String header, URLConnection from,
            HttpServletResponse to) {
        String value = from.getHeaderField(header);

        if (value != null) {
            to.setHeader(header, value);
        }
    }

    private void forward(String url, HttpServletResponse resp)
            throws IOException {
        URLConnection con = new URL(url).openConnection();
        con.connect();

        set_header_if_exists("Last-Modified", con, resp);
        set_header_if_exists("Content-Type", con, resp);
        set_header_if_exists("Content-Length", con, resp);

        InputStream is = con.getInputStream();
        OutputStream os = resp.getOutputStream();

        copy(is, os);
    }
}
