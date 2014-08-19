package rosa.archive.core.check;

import com.google.inject.Inject;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

/**
 *
 */
public class DataChecker implements Checker<Object> {

    @Inject
    private Checker<Book> bookChecker;
    @Inject
    private Checker<BookCollection> collectionChecker;

    DataChecker() {  }

    DataChecker(Checker<Book> bookChecker, Checker<BookCollection> collectionChecker) {
        this.bookChecker = bookChecker;
        this.collectionChecker = collectionChecker;
    }

    @Override
    public boolean checkBits(Object o) {

        if (o instanceof BookCollection) {
            return collectionChecker.checkBits((BookCollection) o);
        } else if (o instanceof Book) {
            return bookChecker.checkBits((Book) o);
        }
        // TODO throw exception for unexpected data type?
        return false;
    }

    @Override
    public boolean checkContent(Object o) {

        if (o instanceof BookCollection) {
            return collectionChecker.checkContent((BookCollection) o);
        } else if (o instanceof Book) {
            return bookChecker.checkContent((Book) o);
        }

        return false;
    }

}
