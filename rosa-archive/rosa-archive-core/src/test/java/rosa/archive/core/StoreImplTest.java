package rosa.archive.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.TemporaryFolder;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

/**
 * Test StoreImpl functionality not tested elsewhere.
 */
public class StoreImplTest extends BaseArchiveTest {
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void testListCollections() throws IOException {
        String[] collections = store.listBookCollections();

        assertNotNull(collections);
        assertEquals(1, collections.length);
        assertEquals(VALID_COLLECTION, collections[0]);
    }

    /**
     * Add a new 'collection' to the temp archive called 'incomplete.ignore'
     * This collection should be ignored when all collections are being listed
     * by the store.
     *
     * @throws IOException
     */
    @Test
    public void listCollectionsIgnoreTest() throws IOException {
        Files.createDirectory(basePath.resolve("incomplete.ignore"));

        assertEquals(2, base.listByteStreamGroupNames().size());
        assertTrue(base.listByteStreamGroupNames().contains("incomplete.ignore"));

        testListCollections();
        assertFalse(Arrays.asList(store.listBookCollections()).contains("incomplete.ignore"));
    }

    @Test
    public void testListBooks() throws IOException {
        String[] books = store.listBooks(VALID_COLLECTION);
        assertNotNull(books);

        List<String> list = Arrays.asList(books);

        assertEquals(2, list.size());
        assertTrue(list.contains(VALID_BOOK_FOLGERSHA2));
        assertTrue(list.contains(VALID_BOOK_LUDWIGXV7));
    }

    /**
     * Add a new book to the valid collection called 'incomplete.ignore'. This book
     * should be be included in the books list for the valid collection when the
     * listBooks method is called.
     *
     * @throws IOException
     */
    @Test
    public void listBooksIgnoreTest() throws IOException {
        Files.createDirectory(basePath.resolve(VALID_COLLECTION).resolve("incomplete.ignore"));

        assertEquals(3, base.getByteStreamGroup(VALID_COLLECTION).listByteStreamGroupNames().size());
        assertTrue(base.getByteStreamGroup(VALID_COLLECTION).listByteStreamGroupNames().contains("incomplete.ignore"));

        testListBooks();
        assertFalse(Arrays.asList(store.listBooks(VALID_COLLECTION)).contains("incomplete.ignore"));
    }

    @Test
    public void testCheckers() throws Exception {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        store.generateTEITranscriptions(VALID_COLLECTION, VALID_BOOK_LUDWIGXV7, errors, warnings);
        store.updateChecksum(loadValidCollection(), loadValidLudwigXV7(), false, errors);
        assertTrue(errors.isEmpty());

        assertTrue(store.check(loadValidCollection(), false, errors, warnings));
        assertEquals(0, errors.size());

        assertTrue(store.check(loadValidCollection(), true, errors, warnings));
        assertEquals(0, errors.size());

        assertTrue(store.check(loadValidCollection(), loadValidLudwigXV7(), false, errors, warnings));
        assertEquals(0, errors.size());

        assertTrue(store.check(loadValidCollection(), loadValidLudwigXV7(), true, errors, warnings));
        assertEquals(0, errors.size());

        assertTrue(store.check(loadValidCollection(), loadValidFolgersHa2(), false, errors, warnings));
        assertEquals(0, errors.size());

        assertTrue(store.check(loadValidCollection(), loadValidFolgersHa2(), true, errors, warnings));
        assertEquals(0, errors.size());
    }

    @Test
    public void testLoadValidCollection() throws Exception {
        BookCollection col = loadValidCollection();

        assertNotNull(col);
        assertNotNull(col.getId());
        assertNotNull(col.getCharacterNames());
        assertNotNull(col.getIllustrationTitles());
        assertNotNull(col.getNarrativeSections());
        assertNotNull(col.getAllSupportedLanguages());
        assertNotNull(col.getBooksRef());
        assertNotNull(col.getLocationsRef());
        assertNotNull(col.getPeopleRef());
    }

    @Test
    public void testLoadValidLudwigXV7() throws Exception {
        Book book = loadValidLudwigXV7();

        assertNotNull(book);
        assertNotNull(book.getId());
        assertNotNull(book.getIllustrationTagging());
        assertNotNull(book.getBookMetadata("en"));
        assertNotNull(book.getBookMetadata("fr"));
        assertNotNull(book.getBookDescription("en"));
        assertNotNull(book.getBookDescription("fr"));
        assertNotNull(book.getContent());
        assertNotNull(book.getCropInfo());
        assertNotNull(book.getChecksum());
        assertNotNull(book.getTranscription());
        assertNotNull(book.getPermission("en"));
        assertNotNull(book.getPermission("fr"));
        assertNotNull(book.getImages());
    }

    /**
     * Try to load a collection that is not in the archive. The operation should
     * complete and return NULL with no exceptions thrown.
     *
     * NOTE: the built in loadCollection() method already does a NotNull assertion
     * before returning. Must use store.loadBookCollection() instead.
     *
     * @throws IOException .
     */
    @Test
    public void testLoadCollectionNotInArchive() throws IOException {
        List<String> errors = new ArrayList<>();

        assertNull("Result should be NULL.", store.loadBookCollection("INVALID COLLECTION", errors));
        assertEquals("There should be exactly ONE error.", 1, errors.size());
    }

    /**
     * Test the method {@link Store#shallowCopy(String, ByteStreamGroup)}.
     * Using NULL as the value for the collection, this method should copy metadata from the
     * whole archive to the target ByteStreamGroup.
     *
     * If the test Store is shallow copied to a temp directory called "tarchive",
     * "tarchive" should have one directory called "archive" which will be a copy of the test
     * Store archive. There should be no images in the copy.
     *
     * @throws Exception .
     */
    @Test
    public void testShallowCopy() throws Exception {
        Path targetPath = tmp.newFolder("tarchive").toPath();
        store.shallowCopy(null, new FSByteStreamGroup(targetPath));

        // Inspect target directory
        for (String collection : store.listBookCollections()) {
            Path tmpColPath = targetPath.resolve("archive").resolve(collection);
            assertTrue("Collection not found in copy. (" + collection + ")",Files.exists(tmpColPath));

            for (String book : store.listBooks(collection)) {
                Path tmpBookPath = tmpColPath.resolve(book);
                assertTrue("Book not found in collection. (" + book + ")", Files.exists(tmpBookPath));
                assertTrue("No files were copied for this book. (" + book + ")",
                        Files.list(tmpBookPath).count() > 0);
                assertTrue("No image files should be present in copy.",
                        Files.list(tmpBookPath).noneMatch(file -> file.getFileName().toString().endsWith(".tif")));
            }
        }
    }
}
