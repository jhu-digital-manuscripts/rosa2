package rosa.iiif.presentation.core;

import rosa.iiif.image.core.UriUtil;
import rosa.iiif.presentation.model.PresentationRequest;
import rosa.iiif.presentation.model.PresentationRequestType;

/**
 * RECOMMENDED: {scheme}://{host}{/prefix}/{collection/book
 * identifier}/{object}/{object identifier}
 */
public class IIIFRequestFormatter {

    private final String scheme;
    private final String host;
    private final String prefix;
    private final int port;

    public IIIFRequestFormatter(String scheme, String host, String prefix, int port) {
        this.scheme = scheme;
        this.host = host;
        this.prefix = prefix;
        this.port = port;
    }

    private String base() {
        return scheme + "://" + host + (port == -1 ? "" : ":" + port) + prefix + "/";
    }

    public String format(PresentationRequest req) {
        PresentationRequestType type = req.getType();

        if (type == PresentationRequestType.COLLECTION) {
            return base() + UriUtil.encodePathSegment(type.getKeyword()) + "/"
                    + UriUtil.encodePathSegment(req.getName());
        } else if (type == PresentationRequestType.MANIFEST) {
            return base() + UriUtil.encodePathSegment(req.getId()) + "/" + UriUtil.encodePathSegment(type.getKeyword());
        } else {
            return base() + UriUtil.encodePathSegment(req.getId()) + "/" + UriUtil.encodePathSegment(req.getName())
                    + "/" + UriUtil.encodePathSegment(req.getName());
        }
    }
}
