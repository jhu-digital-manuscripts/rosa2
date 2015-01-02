package rosa.archive.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

/**
 * Sets up a test store which can be written to containing one collection and
 * one book.
 */

public abstract class BaseStoreTest extends BaseGuiceTest {
    protected final static String COLLECTION_NAME = "rosedata";
    protected final static String BOOK_NAME = "LudwigXV7";

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    protected Path testArchivePath;
    protected Path testCollectionPath;
    protected Path testBookPath;
    protected Store testStore;
    protected ByteStreamGroup testBase;

    protected BookCollection testCollection;
    protected Book testBook;

    @Before
    public void setupArchiveStore() throws URISyntaxException, IOException {
        super.setupArchiveStore();

        // Create test archive in temporary directory

        testArchivePath = tempFolder.newFolder().toPath();
        testCollectionPath = Files.createDirectory(testArchivePath.resolve(COLLECTION_NAME));
        testBookPath = Files.createDirectory(testCollectionPath.resolve(BOOK_NAME));
        testBase = new FSByteStreamGroup(testArchivePath);
        testStore = new StoreImpl(serializers, bookChecker, collectionChecker, testBase);

        // Copy specified collection and book into test archive

        try (DirectoryStream<Path> paths = Files.newDirectoryStream(basePath.resolve(COLLECTION_NAME))) {
            for (Path path : paths) {
                if (Files.isRegularFile(path)) {
                    Files.copy(path, testCollectionPath.resolve(path.getFileName()));
                } else if (path.getFileName().equals(testBookPath.getFileName())) {
                    try (DirectoryStream<Path> book_paths = Files.newDirectoryStream(path)) {
                        for (Path book_path : book_paths) {
                            if (Files.isRegularFile(book_path)) {
                                Files.copy(book_path, testBookPath.resolve(book_path.getFileName()));
                            }
                        }
                    }
                }
            }
        }

        List<String> errors = new ArrayList<>();
        testCollection = store.loadBookCollection(COLLECTION_NAME, errors);
        assertNotNull(testCollection);
        assertEquals(errors.toString(), 0, errors.size());

        testBook = store.loadBook(COLLECTION_NAME, BOOK_NAME, errors);
        assertNotNull(testBook);
        
        assertEquals(errors.toString(), 0, errors.size());
    }

    protected void removeBookFile(String name) throws IOException {
        Files.deleteIfExists(testBookPath.resolve(name));
    }
    
    protected void removeCollectionFile(String name) throws IOException {
        Files.deleteIfExists(testCollectionPath.resolve(name));
    }
}
