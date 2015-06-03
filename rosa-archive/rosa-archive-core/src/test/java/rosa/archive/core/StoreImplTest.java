package rosa.archive.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

/**
 * Test StoreImpl functionality not tested elsewhere.
 */
public class StoreImplTest extends BaseArchiveTest {
    @Test
    public void testListCollections() throws IOException {
        String[] collections = store.listBookCollections();

        assertNotNull(collections);
        assertEquals(1, collections.length);
        assertEquals(VALID_COLLECTION, collections[0]);
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

    @Test
    public void testCheckers() throws Exception {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

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
}
