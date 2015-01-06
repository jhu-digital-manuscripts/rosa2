package rosa.archive.core;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

/**
 * Sets up a temp store containing all the data in BaseGuiceTest store.
 */

public abstract class BaseTmpStoreTest extends BaseGuiceTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    protected Path tmpBasePath;
    protected ByteStreamGroup tmpBase;
    protected Store tmpStore;

    @Before
    public void setupArchiveStore() throws URISyntaxException, IOException {
        super.setupArchiveStore();

        // Create test archive in temporary directory

        tmpBasePath = tempFolder.newFolder().toPath();
        FileUtils.copyDirectory(basePath.toFile(), tmpBasePath.toFile());

        
        tmpBase = new FSByteStreamGroup(tmpBasePath);
        tmpStore = new StoreImpl(serializers, bookChecker, collectionChecker, tmpBase);
    }

    public Path getTmpCollectionPath(String collection) {
        return tmpBasePath.resolve(collection);
    }

    public Path getTmpBookPath(String collection, String book) {
        return tmpBasePath.resolve(collection).resolve(book);
    }

    protected void removeBookFile(String collection, String book, String name) throws IOException {
        Files.deleteIfExists(tmpBasePath.resolve(collection).resolve(book).resolve(name));
    }

    protected void removeCollectionFile(String collection, String name) throws IOException {
        Files.deleteIfExists(tmpBasePath.resolve(collection).resolve(name));
    }

    protected BookCollection loadTmpCollection(String name) throws IOException {
        return loadCollection(tmpStore, name);
    }

    protected Book loadTmpBook(String collection, String name) throws IOException {
        return loadBook(tmpStore, collection, name);
    }

    protected BookCollection loadTmpValidCollection() throws IOException {
        return loadCollection(tmpStore, VALID_COLLECTION_NAME);
    }

    protected Book loadTmpValidCollectionBook(String name) throws IOException {
        return loadBook(tmpStore, VALID_COLLECTION_NAME, name);
    }
}
