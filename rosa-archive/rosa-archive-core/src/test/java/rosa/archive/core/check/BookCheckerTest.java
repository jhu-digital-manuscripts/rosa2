package rosa.archive.core.check;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rosa.archive.model.Book;

import static org.junit.Assert.assertFalse;

/**
 * @see rosa.archive.core.check.BookChecker
 */
public class BookCheckerTest {

    private Checker<Book> bookChecker;

    @Mock
    private Book book;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        this.bookChecker = new BookChecker();
    }

    @Test
    public void checkBitsTest() {
        assertFalse(bookChecker.checkBits(book));
    }

    @Test
    public void checkContentTest() {
        assertFalse(bookChecker.checkContent(book));
    }

}
