package rosa.iiif.presentation.core;

import rosa.iiif.image.core.UriUtil;
import rosa.iiif.presentation.model.PresentationRequest;
import rosa.iiif.presentation.model.PresentationRequestType;

/**
 * {scheme}://{host}{/prefix}/{collection}/{book}/{object}
 */
public class IIIFPresentationRequestFormatter {

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
    public IIIFPresentationRequestFormatter(String scheme, String host, String prefix, int port) {
        this.scheme = scheme;
        this.host = host;
        this.prefix = prefix;
        this.port = port;
    }

    private String base() {
        return scheme + "://" + host + (port == -1 || (scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443) ? "" : ":" + port) + prefix + "/";
    }

    /**
     * Format the presentation request as a URI.
     *
     * @param req request
     * @return URI formatted request
     */
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
