package rosa.iiif.presentation.core.extras;

import org.junit.Test;
import rosa.archive.core.BaseArchiveTest;
import rosa.archive.model.BookReferenceSheet.Link;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class BookReferenceResourceDbTest extends BaseArchiveTest {

    /**
     * Book called "Tragoedia" has a Perseus link in the current books.csv
     *
     * @throws Exception .
     */
    @Test
    public void perseusTest() throws Exception {
        final String book = "Tragoedia";
        BookReferenceResourceDb db = new BookReferenceResourceDb(Link.PERSEUS, loadValidCollection().getBooksRef());

//        BookReferenceSheet sheet = loadValidCollection().getBooksRef();
//        System.out.println(sheet.getKeys());
//        sheet.getKeys().forEach(key -> System.out.println("  > " + key + " :: " + sheet.getExternalLinks(key)));

        assertNotNull(db.lookup(book));
        assertFalse(db.lookup(book).toString().isEmpty());
    }

}
