package rosa.iiif.presentation.core.extres;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

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
        db.put(normalize(term), res);
    }
}
