package rosa.archive.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import rosa.archive.core.GuiceJUnitRunner.GuiceModules;
import rosa.archive.core.check.BookChecker;
import rosa.archive.core.check.BookCollectionChecker;
import rosa.archive.core.serialize.SerializerSet;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

import com.google.inject.Inject;

/**
 * Setup Guice injection and for each test create a Store which points to the archive in
 * src/test/resources/archive.
 */
@RunWith(GuiceJUnitRunner.class)
@GuiceModules({ ArchiveCoreModule.class })
public abstract class BaseArchiveTest {
    protected final static String VALID_COLLECTION = "valid";
    protected final static String VALID_BOOK_FOLGERSHA2 = "FolgersHa2";
    protected final static String VALID_BOOK_LUDWIGXV7 = "LudwigXV7";
    protected final static String[] VALID_COLLECTION_BOOKS = { VALID_BOOK_LUDWIGXV7, VALID_BOOK_FOLGERSHA2 };

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Inject
    protected SerializerSet serializers;

    @Inject
    protected BookChecker bookChecker;

    @Inject
    protected BookCollectionChecker collectionChecker;

    protected ByteStreamGroup base;
    protected Path basePath;
    protected StoreImpl store;
    protected SimpleStore simpleStore;

    @Before
    public void setupArchiveStore() throws Exception {
        Path tmp = tempFolder.newFolder().toPath();
        
        ResourceUtil.copyResource(getClass(), "/archive", tmp);
        
        basePath = tmp.resolve("archive");
        base = new FSByteStreamGroup(basePath);
        store = new StoreImpl(serializers, bookChecker, collectionChecker, base, true);
        simpleStore = new SimpleCachingStore(store, 1000);
    }

    protected BookCollection loadCollection(String name) throws IOException {
        List<String> errors = new ArrayList<>();

        BookCollection result = store.loadBookCollection(name, errors);

        assertNotNull(result);
        assertEquals(0, errors.size());

        return result;
    }

    protected Book loadBook(String collection, String book) throws IOException {
        List<String> errors = new ArrayList<>();

        Book result = store.loadBook(loadCollection(collection), book, errors);

        assertNotNull(result);
        assertEquals(0, errors.size());

        return result;
    }

    protected BookCollection loadValidCollection() throws IOException {
        return loadCollection(VALID_COLLECTION);
    }

    protected Book loadValidCollectionBook(String name) throws IOException {
        return loadBook(VALID_COLLECTION, name);
    }

    protected Book loadValidFolgersHa2() throws IOException {
        return loadBook(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2);
    }

    protected Book loadValidLudwigXV7() throws IOException {
        return loadBook(VALID_COLLECTION, VALID_BOOK_LUDWIGXV7);
    }
    
    public Path getCollectionPath(String collection) {
        return basePath.resolve(collection);
    }

    public Path getBookPath(String collection, String book) {
        return basePath.resolve(collection).resolve(book);
    }

    protected void removeBookFile(String collection, String book, String name) throws IOException {
        Files.deleteIfExists(basePath.resolve(collection).resolve(book).resolve(name));
    }

    protected void removeCollectionFile(String collection, String name) throws IOException {
        Files.deleteIfExists(basePath.resolve(collection).resolve(name));
    }

}
