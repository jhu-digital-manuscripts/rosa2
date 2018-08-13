package rosa.iiif.presentation.core.extras;

import java.net.URI;
import java.util.function.Consumer;

/**
 * Set of terms mapped to resources.
 */
public interface ExternalResourceDb {
    /**
     * Return all resources mapped to a term.
     *
     * @param term
     * @param consumer
     */
    void lookup(String term, Consumer<URI> consumer);

    /**
     * @param term
     * @return a resource for a term or null if none exists
     */

    URI lookup(String term);

    String label();
}