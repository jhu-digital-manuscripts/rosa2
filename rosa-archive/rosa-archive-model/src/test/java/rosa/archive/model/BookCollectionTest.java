package rosa.archive.model;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @see BookCollection
 */
public class BookCollectionTest {
    private static final int MAX_BOOKS = 10;
    private static final String[] LANGS = { "en", "fr", "de" };

    private BookCollection collection;

    @Before
    public void setup() {
        this.collection = new BookCollection();

        // Setting the books in the collection.
        HashMap<String, Book> books = new HashMap<>();
        for (int i = 0; i < MAX_BOOKS; i++) {
            Book book = mock(Book.class);
            when(book.getId()).thenReturn(String.valueOf(i));

            books.put(book.getId(), book);
        }
        collection.setBooks(books);

        // Add supported languages
        for (String lang : LANGS) {
            collection.addSupportedLanguage(lang);
        }
    }

    @Test
    public void getAllBookIdsWorks() {
        Set<String> bookIdsFromCollection = collection.getAllBookIds();

        assertNotNull(bookIdsFromCollection);
        assertEquals(MAX_BOOKS, bookIdsFromCollection.size());
        for (int i = 0; i < MAX_BOOKS; i++) {
            assertTrue(bookIdsFromCollection.contains(String.valueOf(i)));
        }

    }

    @Test
    public void getAllBooksWorks() {
        Set<Book> allBooks = collection.getAllBooks();

        assertNotNull(allBooks);
        assertEquals(MAX_BOOKS, allBooks.size());
        for (Book book : allBooks) {
            int id = Integer.parseInt(book.getId());
            assertTrue(id >= 0 && id < MAX_BOOKS);
        }
    }

    @Test
    public void emptySetReturnedIfNoBooksPresent() {
        BookCollection collection = new BookCollection();

        Set<Book> allBooks = collection.getAllBooks();
        assertNotNull(allBooks);
        assertTrue(allBooks.isEmpty());
    }

    @Test
    public void getBookWorksForPresentId() {
        String[] queries = { "1", "5", "8" };

        for (String id : queries) {
            Book book = collection.getBook(id);
            assertNotNull(book);
            assertEquals(id, book.getId());
        }
    }

    @Test
    public void getBookReturnsNullForAbsentId() {
        String[] queries = { "-2", "15", "100" };

        for (String id : queries) {
            Book book = collection.getBook(id);
            assertNull(book);
        }
    }

    @Test
    public void languageIsSupported() {
        assertTrue(collection.isLanguageSupported(LANGS[0]));
        assertTrue(collection.isLanguageSupported(LANGS[1]));
        assertTrue(collection.isLanguageSupported(LANGS[2]));
    }

    @Test
    public void languageNotSupported() {
        assertFalse(collection.isLanguageSupported("asdf"));
        assertFalse(collection.isLanguageSupported("is"));
    }

}
