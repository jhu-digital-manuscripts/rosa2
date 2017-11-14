package rosa.iiif.presentation.core.extres;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Simple in memory implementation that ignores the issue of terms with multiple
 * resources. The first term added is used.
 */
public class SimpleExternalResourceDb implements ExternalResourceDb {
    private final Map<String, URI> db;

    public SimpleExternalResourceDb() {
        db = new HashMap<String, URI>();
    }

    @Override
    public URI lookup(String term) {
        return db.get(normalize(term));
    }

    @Override
    public void lookup(String term, Consumer<URI> consumer) {
        URI res = lookup(term);

        if (res != null) {
            consumer.accept(res);
        }
    }

    protected String normalize(String term) {
        return term;
    }

    public void add(String term, URI res) {
        db.putIfAbsent(normalize(term), res);
    }
}
