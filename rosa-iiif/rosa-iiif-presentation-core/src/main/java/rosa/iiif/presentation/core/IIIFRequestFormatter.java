package rosa.iiif.presentation.core;

import rosa.iiif.image.core.UriUtil;
import rosa.iiif.presentation.model.PresentationRequest;
import rosa.iiif.presentation.model.PresentationRequestType;

/**
 * RECOMMENDED: {scheme}://{host}{/prefix}/{collection.book}/{object}/{object identifier}
 *              {scheme}://{host}{/prefix}/{id}             /{type}  /{name}
 */
public class IIIFRequestFormatter {

    private final String scheme;
    private final String host;
    private final String prefix;
    private final int port;

    /**
     * @param scheme
     *            http or https
     * @param host
     * @param port
     *            -1 for default port
     * @param prefix
     *            must be encoded, start with '/', and not end with '/'.
     */
    public IIIFRequestFormatter(String scheme, String host, String prefix, int port) {
        this.scheme = scheme;
        this.host = host;
        this.prefix = prefix;
        this.port = port;
    }

    private String base() {
        return scheme + "://" + host + (port == -1 || (scheme.equals("http") && port == 80) ? "" : ":" + port) + prefix + "/";
    }

    public String format(PresentationRequest req) {
        PresentationRequestType type = req.getType();

        if (type == PresentationRequestType.COLLECTION) {
            return base() + UriUtil.encodePathSegment(type.getKeyword()) + "/"
                    + UriUtil.encodePathSegment(req.getName());
        } else if (type == PresentationRequestType.MANIFEST) {
            return base() + UriUtil.encodePathSegment(req.getId()) + "/" + UriUtil.encodePathSegment(type.getKeyword());
        } else {
            return base() + UriUtil.encodePathSegment(req.getId()) + "/" + UriUtil.encodePathSegment(type.getKeyword())
                    + "/" + UriUtil.encodePathSegment(req.getName());
        }
    }
}
