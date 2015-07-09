package rosa.website.core.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import rosa.archive.core.Store;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.website.viewer.server.FSISerializer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
    private static final Logger logger = Logger.getLogger(FSIDataServlet.class.toString());

    private static final int MAX_CACHE_SIZE = 100;
    private static final ConcurrentMap<String, Book> bookCache = new ConcurrentHashMap<>(MAX_CACHE_SIZE);
    private static final ConcurrentMap<String, BookCollection> collectionCache = new ConcurrentHashMap<>(MAX_CACHE_SIZE);

    private static final String TYPE_PAGES = "pages.fsi";
    private static final String TYPE_SHOWCASE = "showcase.fsi";

    private static final String XML_MIME_TYPE = "application/xml";

    private Store archiveStore;
    private FSISerializer serializer;

    @Inject
    public FSIDataServlet(StoreProvider storeProvider, @Named("archive.path") String archivePath,
                          FSISerializer serializer) {
        this.serializer = serializer;
        this.archiveStore = storeProvider.getStore(archivePath);
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

        Book book = loadBook(collection, bookName);

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

    private BookCollection loadBookCollection(String collection) throws IOException {
        BookCollection col = collectionCache.get(collection);
        if (col != null) {
            return col;
        }

        List<String> errors = new ArrayList<>();

        try {
            col = archiveStore.loadBookCollection(collection, errors);
            checkErrors(errors);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "An error has occurred while loading a collection. [" + collection + "]", e);
        }

        if (collectionCache.size() >= MAX_CACHE_SIZE) {
            collectionCache.clear();
        }
        collectionCache.putIfAbsent(collection, col);

        return col;
    }

    private Book loadBook(String collection, String book) throws IOException {
        String key = collection + "." + book;

        Book b = bookCache.get(key);
        if (b != null) {
            return b;
        }

        b = loadBook(loadBookCollection(collection), book);
        if (bookCache.size() >= MAX_CACHE_SIZE) {
            bookCache.clear();
        }
        bookCache.putIfAbsent(key, b);

        return b;
    }

    private Book loadBook(BookCollection collection, String book) throws IOException {
        List<String> errors = new ArrayList<>();
        Book b = null;

        try {
            b = archiveStore.loadBook(collection, book, errors);
            checkErrors(errors);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "An error has occurred while loading a book. [" + collection + ":" + book + "]", e);
        }

        return b;
    }

    private void checkErrors(List<String> errors) {
        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder("Error loading book collection.\n");
            for (String s : errors) {
                sb.append(s);
                sb.append('\n');
            }
            logger.warning(sb.toString());
        }
    }

}
