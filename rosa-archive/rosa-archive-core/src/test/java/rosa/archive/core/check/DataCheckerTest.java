package rosa.archive.core.check;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @see rosa.archive.core.check.DataChecker
 */
public class DataCheckerTest {

    private Checker<Object> checker;
    @Mock
    private Checker<Book> bookChecker;
    @Mock
    private Checker<BookCollection> collectionChecker;

    private Book book;
    private BookCollection collection;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(bookChecker.checkBits(any(Book.class))).thenReturn(false);
        when(bookChecker.checkContent(any(Book.class))).thenReturn(false);
        when(collectionChecker.checkBits(any(BookCollection.class))).thenReturn(false);
        when(collectionChecker.checkContent(any(BookCollection.class))).thenReturn(false);

        this.book = new Book();
        this.collection = new BookCollection();
        this.checker = new DataChecker(bookChecker, collectionChecker);
    }

    @Test
    public void bookTest() {
        assertFalse(checker.checkBits(book));
        assertFalse(checker.checkContent(book));

        verify(bookChecker).checkBits(book);
        verify(bookChecker).checkContent(book);
        verify(collectionChecker, never()).checkBits(collection);
        verify(collectionChecker, never()).checkContent(collection);
    }

    @Test
    public void collectionTest() {
        assertFalse(checker.checkBits(collection));
        assertFalse(checker.checkContent(collection));

        verify(collectionChecker).checkBits(collection);
        verify(collectionChecker).checkContent(collection);
        verify(bookChecker, never()).checkBits(book);
        verify(bookChecker, never()).checkContent(book);
    }

}
