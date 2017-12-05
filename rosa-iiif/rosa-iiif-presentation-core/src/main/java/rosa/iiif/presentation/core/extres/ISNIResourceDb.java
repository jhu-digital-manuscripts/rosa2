package rosa.iiif.presentation.core.extres;

import org.apache.commons.lang3.StringUtils;
import rosa.archive.model.BookCollection;
import rosa.archive.model.ReferenceSheet;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ISNIResourceDb implements ExternalResourceDb {
    private static final Logger logger = Logger.getLogger("ISNIResourceDb");
    private static final String ISNI_URL = "http://isni.org/isni/";

    private final Map<String, URI> db;

    public ISNIResourceDb(BookCollection collection) {
        db = new ConcurrentHashMap<>();
        setCollection(collection);
    }

    /*
     * Reset the current book collection. This well empty and refill the database
     * according to the 'people' spreadsheet available in the collection.
     */
    private void setCollection(BookCollection collection) {
        db.clear();

        ReferenceSheet people = collection.getPeopleRef();
        if (people != null) {
            people.getKeys().parallelStream().forEach(name ->
                    people.getAlternates(name).stream()
                            .map(n -> n.replaceAll("\\s+", ""))  // Remove spaces
                            .filter(this::isPossibleISNI)
                            .findAny()
                            .ifPresent(n -> {
                                try {
                                    db.putIfAbsent(normalize(name), new URI(ISNI_URL + n));
                                } catch (URISyntaxException e) {
                                    logger.log(Level.WARNING, "Failed to generate ISNI URI for person (" + name + ")", e);
                                }
                            })
                    );
        }
    }

    // Remove punctuation and trailing spaces
    public String normalize(String s) {
        return s.replaceAll("\\p{Punct}+", "").toLowerCase().trim();
    }

    private boolean isPossibleISNI(String str) {
        if (str == null || str.length() < 2) {
            return false;
        }
        return StringUtils.isNumeric(str.substring(0, str.length() - 2));
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
        return db.get(normalize(term));
    }
}
