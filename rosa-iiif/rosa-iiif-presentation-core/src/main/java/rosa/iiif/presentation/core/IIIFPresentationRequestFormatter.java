package rosa.iiif.presentation.core;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import rosa.iiif.image.core.UriUtil;
import rosa.iiif.presentation.model.PresentationRequest;
import rosa.iiif.presentation.model.PresentationRequestType;
import rosa.iiif.presentation.model.search.IIIFSearchRequest;
import rosa.iiif.presentation.model.search.Rectangle;

/**
 * RECOMMENDED: {scheme}://{host}{/prefix}/{collection.book}/{object}/{object identifier}
 *              {scheme}://{host}{/prefix}/{id}             /{type}  /{name}
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
        return scheme + "://" + host + (port == -1 || (scheme.equals("http") && port == 80) ? "" : ":" + port) + prefix + "/";
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
    
    public String format(IIIFSearchRequest request) {
        StringBuilder uri = new StringBuilder(format(request.objectId));

        // TODO FIX
        uri.append("/search?q=");
        uri.append(arrayToString(request.queryTerms));

        if (!arrayEmpty(request.motivations)) {
            uri.append("&motivation=");
            uri.append(arrayToString(request.motivations));
        }
        if (!arrayEmpty(request.users)) {
            uri.append("&user=");
            uri.append(arrayToString(request.users));
        }
        if (!arrayEmpty(request.dates)) {
            uri.append("&date=");
            uri.append(arrayToString(request.dates));
        }
        if (!arrayEmpty(request.box)) {
            uri.append("&box=");
            uri.append(arrayToString(request.box));
        }

        if (request.page > 0) {
            uri.append("&page=");
            uri.append(String.valueOf(request.page));
        }

        return uri.toString();
    }

    private String arrayToString(String[] arr) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < arr.length; i++) {
            if (i != 0) {
                sb.append("%20");
            }
            try {
                sb.append(URLEncoder.encode(arr[i], "UTF-8"));
            } catch (UnsupportedEncodingException e) {

            }
        }

        return sb.toString();
    }

    private String arrayToString(Rectangle[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i != 0) {
                sb.append("%20");
            }
            sb.append(String.valueOf(arr[i].x));
            sb.append(',');
            sb.append(String.valueOf(arr[i].y));
            sb.append(',');
            sb.append(String.valueOf(arr[i].width));
            sb.append(',');
            sb.append(String.valueOf(arr[i].height));
        }
        return sb.toString();
    }

    private boolean arrayEmpty(Object[] arr) {
        return arr == null || arr.length == 0;
    }
}
