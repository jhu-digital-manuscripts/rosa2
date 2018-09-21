package rosa.iiif.presentation.core;

import com.google.inject.Inject;
import rosa.iiif.image.core.UriUtil;

public class StaticResourceRequestFormatter {
    private final String scheme;
    private final String host;
    private final String prefix;
    private final int port;

    /**
     * @param scheme
     *            http or https
     * @param host the host
     * @param port
     *            -1 for default port
     * @param prefix
     *            must be encoded, start with '/', and not end with '/'.
     */
    @Inject
    public StaticResourceRequestFormatter(String scheme, String host, String prefix, int port) {
        this.scheme = scheme;
        this.host = host;
        this.prefix = prefix;
        this.port = port;
    }

    private String base() {
        return scheme + "://" + host + (port == -1 || (scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443) ? "" : ":" + port) + prefix + "/";
    }

    /**
     * Get a URI that points to a static resource that has been exposed.
     *
     * @param collection name of collection
     * @param book name of parent book (can be NULL if target is in a Collection)
     * @param target name of target file
     * @return URI to the target
     */
    public String format(String collection, String book, String target) {
        StringBuilder result = new StringBuilder(base());

        result.append(UriUtil.encodePathSegment(collection)).append('/');
        if (book != null && !book.isEmpty()) {
            result.append(UriUtil.encodePathSegment(book)).append('/');
        }
        result.append(UriUtil.encodePathSegment(target));

        return result.toString();
    }

}
