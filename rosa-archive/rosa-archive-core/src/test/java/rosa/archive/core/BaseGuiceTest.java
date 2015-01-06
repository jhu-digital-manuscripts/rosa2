package rosa.archive.core;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.runner.RunWith;

import rosa.archive.core.GuiceJUnitRunner.GuiceModules;
import rosa.archive.core.check.BookChecker;
import rosa.archive.core.check.BookCollectionChecker;
import rosa.archive.core.serialize.SerializerSet;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

import com.google.inject.Inject;

/**
 * Setup Guice injection and a read only store which points to the archive in
 * src/test/resources/archive.
 */
@RunWith(GuiceJUnitRunner.class)
@GuiceModules({ ArchiveCoreModule.class })
public abstract class BaseGuiceTest {
    protected final static String VALID_COLLECTION_NAME = "valid";
    protected final static String VALID_BOOK_FOLGERSHA2_NAME = "FolgersHa2";
    protected final static String VALID_BOOK_LUDWIGXV7 = "LudwigXV7";

    protected final static String[] VALID_COLLECTION_BOOKS = { VALID_BOOK_LUDWIGXV7, VALID_BOOK_FOLGERSHA2_NAME };

    @Inject
    protected SerializerSet serializers;

    @Inject
    protected BookChecker bookChecker;

    @Inject
    protected BookCollectionChecker collectionChecker;

    protected ByteStreamGroup base;
    protected Path basePath;
    protected StoreImpl store;

    @Before
    public void setupArchiveStore() throws URISyntaxException, IOException {
        URL u = getClass().getClassLoader().getResource("archive");
        assertNotNull(u);

        basePath = Paths.get(u.toURI());
        assertNotNull(basePath);
        assertTrue(Files.isDirectory(basePath));

        base = new FSByteStreamGroup(basePath);
        store = new StoreImpl(serializers, bookChecker, collectionChecker, base);
    }

    protected BookCollection loadCollection(Store store, String name) throws IOException {
        List<String> errors = new ArrayList<>();

        BookCollection result = store.loadBookCollection(name, errors);

        assertNotNull(result);
        assertEquals(0, errors.size());

        return result;
    }

    protected Book loadBook(Store store, String collection, String book) throws IOException {
        List<String> errors = new ArrayList<>();

        Book result = store.loadBook(loadCollection(store, collection), book, errors);

        assertNotNull(result);
        assertEquals(0, errors.size());

        return result;
    }

    protected BookCollection loadValidCollection() throws IOException {
        return loadCollection(store, VALID_COLLECTION_NAME);
    }

    protected Book loadValidCollectionBook(String name) throws IOException {
        return loadBook(store, VALID_COLLECTION_NAME, name);
    }

    protected Book loadValidFolgersHa2() throws IOException {
        return loadBook(store, VALID_COLLECTION_NAME, VALID_BOOK_FOLGERSHA2_NAME);
    }

    protected Book loadValidLudwigXV7() throws IOException {
        return loadBook(store, VALID_COLLECTION_NAME, VALID_BOOK_LUDWIGXV7);
    }
}
