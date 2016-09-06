package rosa.website.core.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import rosa.archive.model.Book;
import rosa.website.viewer.server.FSISerializer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet will serve FSI XML configs for the 'pages' and 'showcase' viewers based on
 * URL path. Required parameters: collection name, book name, fsi viewer type
 *
 * {@code servlet/collectionName/bookName/(pages|showcase)}
 */
@Singleton
public class FSIDataServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger.getLogger(FSIDataServlet.class.toString());

    private static final String TYPE_PAGES = "pages.fsi";
    private static final String TYPE_SHOWCASE = "showcase.fsi";

    private static final String XML_MIME_TYPE = "application/xml";

    private StoreAccessLayer store;
    private FSISerializer serializer;

    @Inject
    public FSIDataServlet(StoreAccessLayer store, FSISerializer serializer) {
        this.serializer = serializer;
        this.store = store;
    }

    @Override
    public void init() {

    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Extract parameters: collection, book, type

        String path = URLDecoder.decode(req.getPathInfo(), "UTF-8");
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        String[] parts = path.split("/");

        String collection = parts[0];
        String bookName = parts[1];
        String type = parts[2];

        Book book = store.book(collection, bookName);

        resp.setContentType(XML_MIME_TYPE);
        boolean complete = false;
        String message = "";

        try {
            switch (type) {
                case TYPE_PAGES:
                    complete = serializer.fsiPagesDoc(collection, book, resp.getOutputStream());
                    break;
                case TYPE_SHOWCASE:
                    complete = serializer.fsiShowcaseDoc(collection, book, resp.getOutputStream());
                    break;
                default:
                    message = "Invalid type specified. (" + type + ")";
                    break;
            }
        } catch (XMLStreamException e) {
            message = "Failed to write XML.";
            logger.log(Level.SEVERE, message, e);
        }

        if (!complete) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message);
        }
        resp.flushBuffer();
    }

}
