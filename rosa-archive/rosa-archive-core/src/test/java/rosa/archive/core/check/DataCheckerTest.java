package rosa.archive.core.check;

import com.google.inject.Inject;
import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import rosa.archive.core.ArchiveCoreModule;
import rosa.archive.core.GuiceJUnitRunner;
import rosa.archive.core.GuiceJUnitRunner.GuiceModules;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

import static org.junit.Assert.assertEquals;

/**
 * @see rosa.archive.core.check.DataChecker
 */
@RunWith(GuiceJUnitRunner.class)
@GuiceModules({ArchiveCoreModule.class})
public class DataCheckerTest {

    @Inject
    private Checker<Object> checker;
    @Inject
    private Checker<Book> bookChecker;
    @Inject
    private Checker<BookCollection> collectionChecker;

    // Was used to test Guice DI
    @Ignore
    @Test
    public void test() {
        assertEquals(DataChecker.class, checker.getClass());
        assertEquals(BookChecker.class, bookChecker.getClass());
        assertEquals(BookCollectionChecker.class, collectionChecker.getClass());

        System.out.println("Checker: " + checker.getClass().getSimpleName());
        System.out.println("BookChecker: " + bookChecker.getClass().getSimpleName());
        System.out.println("CollectionChecker: " + collectionChecker.getClass().getSimpleName());
    }

}
