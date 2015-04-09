package rosa.website.core.server;

import com.google.inject.Guice;
import com.google.inject.Injector;
import rosa.archive.core.ArchiveCoreModule;
import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.FSByteStreamGroup;
import rosa.archive.core.Store;
import rosa.archive.core.StoreImpl;
import rosa.archive.core.check.BookChecker;
import rosa.archive.core.check.BookCollectionChecker;
import rosa.archive.core.serialize.SerializerSet;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FSIDataServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(FSIDataServlet.class.toString());
    private static final String FSI_MAP_NAME = "fsi-share-map.properties";

    private static final String PARAM_COLLECTION = "collection";
    private static final String PARAM_BOOK = "book";
    private static final String PARAM_FSI_TYPE = "fsi";
    private static final String TYPE_PAGES = "pages";
    private static final String TYPE_SHOWCASE = "showcase";

    private static final String XML_MIME_TYPE = "application/xml";

    private Store archiveStore;
    private FsiSerializer serializer;

    @Override
    public void init() {
        try (InputStream in = getClass().getResourceAsStream(FSI_MAP_NAME)) {
            Properties props = new Properties();
            props.load(in);

            Map<String, String> prop_map = new HashMap<>();
            for (String key : props.stringPropertyNames()) {
                prop_map.put(key, props.getProperty(key));
            }

            serializer = new FsiSerializer(prop_map);
            logger.info("Loaded FSI share mapping. " + prop_map.toString());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load " + FSI_MAP_NAME, e);
        }


        // setup the Store to point at the archive
        logger.info("Initializing FSIDataService.");
        Injector injector = Guice.createInjector(new ArchiveCoreModule());

        String path = getServletContext().getInitParameter("archive-path");
        if (path == null || path.isEmpty()) {
            logger.warning("'archive-path' not specified. Using default value [/mnt]");
            path = "/mnt";
        }

        ByteStreamGroup base = new FSByteStreamGroup(path);
        this.archiveStore = new StoreImpl(injector.getInstance(SerializerSet.class), injector.getInstance(BookChecker.class),
                injector.getInstance(BookCollectionChecker.class), base);
        logger.info("Archive Store set.");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Extract parameters: collection, book, type
        String collection = req.getParameter(PARAM_COLLECTION);
        String bookName = req.getParameter(PARAM_BOOK);
        String type = req.getParameter(PARAM_FSI_TYPE);

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
        List<String> errors = new ArrayList<>();
        BookCollection col = null;

        try {
            col = archiveStore.loadBookCollection(collection, errors);
            checkErrors(errors);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "An error has occurred while loading a collection. [" + collection + "]", e);
        }

        logger.info("Book collection loaded. [" + collection + "]");
        return col;
    }

    private Book loadBook(String collection, String book) throws IOException {
        return loadBook(loadBookCollection(collection), book);
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
