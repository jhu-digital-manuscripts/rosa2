package rosa.iiif.search.core;

import rosa.iiif.presentation.core.IIIFRequestFormatter;
import rosa.iiif.search.model.IIIFSearchRequest;
import rosa.iiif.search.model.Rectangle;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class IIIFSearchRequestFormatter extends IIIFRequestFormatter {

    public IIIFSearchRequestFormatter(String scheme, String host, String prefix, int port) {
        super(scheme, host, prefix, port);
    }

    public String format(IIIFSearchRequest request) {
        StringBuilder uri = new StringBuilder(format(request.objectId));

        uri.append("?q=");
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
