package rosa.iiif.presentation.core.extras;

import rosa.archive.model.BookReferenceSheet;
import rosa.archive.model.BookReferenceSheet.Link;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.function.Consumer;

public class BookReferenceResourceDb implements ExternalResourceDb {
//    private static final Logger logger = Logger.getLogger("BookReferenceResourceDb");

    private Link type;
    private BookReferenceSheet bookDb;

    public BookReferenceResourceDb(Link type, BookReferenceSheet bookDb) {
        setBookDb(bookDb);
        this.type = type;
    }

    public void setBookDb(BookReferenceSheet bookDb) {
        this.bookDb = bookDb;
    }

    @Override
    public void lookup(String term, Consumer<URI> consumer) {
        URI uri = lookup(term);
        if (uri != null) {
            consumer.accept(uri);
        }
    }

    @Override
    public URI lookup(String term) {
        if (bookDb == null) {
            return null;
        }

        Map<String, String> bookLinks = bookDb.getExternalLinks(term);
        if (bookLinks == null || !bookLinks.containsKey(type.label)) {
            return null;
        }

        try {
            return new URI(bookLinks.get(type.label));
        } catch (URISyntaxException e) {
//            logger.log(Level.WARNING, "");
            return null;
        }
    }

    @Override
    public String label() {
        return type.label;
    }
}
