package rosa.archive.core.check;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rosa.archive.model.BookCollection;

import static org.junit.Assert.assertFalse;

/**
 * @see rosa.archive.core.check.BookCollectionChecker
 */
public class BookCollectionCheckerTest {

    private BookCollectionChecker checker;

    @Mock
    private BookCollection collection;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        this.checker = new BookCollectionChecker();
    }

    @Test
    public void checkBitsTest() {
        assertFalse(checker.checkBits(collection));
    }

    @Test
    public void checkContentsTest() {
        assertFalse(checker.checkContent(collection));
    }

}
