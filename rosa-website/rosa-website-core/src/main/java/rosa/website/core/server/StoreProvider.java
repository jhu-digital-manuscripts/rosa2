package rosa.website.core.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import rosa.archive.core.FSByteStreamGroup;
import rosa.archive.core.Store;
import rosa.archive.core.StoreImpl;
import rosa.archive.core.check.BookChecker;
import rosa.archive.core.check.BookCollectionChecker;
import rosa.archive.core.serialize.SerializerSet;

import java.util.logging.Logger;

@Singleton
public class StoreProvider {
    private static final Logger log = Logger.getLogger(StoreProvider.class.toString());

    private Store CURRENT_STORE;
    private String CURRENT_PATH;

    private SerializerSet serializerSet;
    private BookChecker bookChecker;
    private BookCollectionChecker bookCollectionChecker;

    @Inject
    public StoreProvider(SerializerSet serializerSet, BookChecker bookChecker, BookCollectionChecker bookCollectionChecker) {
        this.serializerSet = serializerSet;
        this.bookChecker = bookChecker;
        this.bookCollectionChecker = bookCollectionChecker;
    }

    public Store getStore(String path) {
        if (CURRENT_STORE == null || path == null || !path.equals(CURRENT_PATH)) {
            setStore(path);
        }

        return CURRENT_STORE;
    }

    private void setStore(String p) {
        log.info("New store at path (" + p + ")");
        CURRENT_PATH = p;
        CURRENT_STORE = new StoreImpl(serializerSet, bookChecker, bookCollectionChecker, new FSByteStreamGroup(p));
    }
}
