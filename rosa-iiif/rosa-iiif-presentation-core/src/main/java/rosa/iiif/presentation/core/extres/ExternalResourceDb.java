package rosa.iiif.presentation.core.extres;

import java.net.URI;
import java.util.function.Consumer;

interface ExternalResourceDb {
    void lookup(String term, Consumer<URI> consumer);
    
    URI lookup(String term);
}
