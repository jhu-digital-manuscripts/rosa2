package rosa.archive.core.check;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

/**
 *
 */
public class DataChecker implements Checker {

    @Override
    public boolean checkBits(Object o) {

        if (o instanceof BookCollection) {

        } else if (o instanceof Book) {

        }

        return false;
    }

    @Override
    public boolean checkContent(Object o) {

        if (o instanceof BookCollection) {

        } else if (o instanceof Book) {

        }

        return false;
    }

}
