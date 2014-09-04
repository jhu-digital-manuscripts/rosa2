package rosa.archive.core.check;

import com.google.inject.Inject;
import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.config.AppConfig;
import rosa.archive.model.BookCollection;

/**
 *
 */
public class BookCollectionChecker implements Checker<BookCollection> {

    private AppConfig config;

    @Inject
    public BookCollectionChecker(AppConfig config) {
        this.config = config;
    }

    @Override
    public boolean checkContent(BookCollection collection, ByteStreamGroup bsg, boolean checkBits) {
        return false;
    }
}
