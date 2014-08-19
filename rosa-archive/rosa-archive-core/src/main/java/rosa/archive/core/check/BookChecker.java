package rosa.archive.core.check;

import rosa.archive.model.Book;

/**
 * Checks data for a {@link rosa.archive.model.Book}
 */
public class BookChecker implements Checker<Book > {

    BookChecker() {  }

    @Override
    public boolean checkBits(Book book) {
        return false;
    }

    @Override
    public boolean checkContent(Book book) {
        return false;
    }
}
