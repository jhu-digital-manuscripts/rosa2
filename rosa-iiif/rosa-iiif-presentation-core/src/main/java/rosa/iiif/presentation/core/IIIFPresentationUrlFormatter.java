package rosa.iiif.presentation.core;

/**
 * RECOMMENDED: {scheme}://{host}{/prefix}/{collection/book identifier}/{object}/{object identifier}
 */
public class IIIFPresentationUrlFormatter {

    private final String scheme;
    private final String host;
    private final String prefix;
    private final int port;

    public IIIFPresentationUrlFormatter(String scheme, String host, String prefix, int port) {
        this.scheme = scheme;
        this.host = host;
        this.prefix = prefix;
        this.port = port;
    }

    private String base() {
        return scheme + "://" + host + (port == -1 ? "" : ":" + port) + prefix + "/";
    }

}
