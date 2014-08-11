package rosa.archive.core.check;

import rosa.archive.model.BookCollection;

/**
 *
 */
public class BookCollectionChecker implements Checker<BookCollection> {
    @Override
    public boolean checkBits(BookCollection collection) {
        return false;
    }

    @Override
    public boolean checkContent(BookCollection collection) {
        return false;
    }
}
