package rosa.iiif.image.core;

import java.net.URI;
import java.net.URISyntaxException;

public class UriUtil {
    /**
     * Return whether or not the given encoded path is a valid URI path.
     * 
     * @param s
     * @return status
     */
    public static boolean isValidEncodedPath(String s) {
        try {
            new URI("http://test" + s).getPath().substring(1);
        } catch (URISyntaxException e) {
            return false;
        }

        return true;
    }

    /**
     * @param s
     * @return string encoded as a URI path segment
     */
    public static String encodePathSegment(String s) {
        try {
            return new URI("http", "test", "/" + s, null).getRawPath().substring(1).replace("/", "%2f");
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failure trying to encode path segment: " + s, e);
        }
    }

    /**
     * @param s
     *            must be valid encoded path segment
     * @return decoded path segment
     */
    public static String decodePathSegment(String s) {
        try {
            return new URI("http://test/" + s).getPath().substring(1);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failure trying to decode path segment: " + s, e);
        }
    }

    /**
     * Splits path into segments and returns them decoded.
     * 
     * @param path
     *            must be valid encoded path
     * @return decoded path segments
     */
    public static String[] decodePathSegments(String path) {
        String[] result = path.split("/");

        for (int i = 0; i < result.length; i++) {
            result[i] = decodePathSegment(result[i]);
        }

        return result;
    }

    /**
     * @param segs
     * @return relative encoded path consisting of given segments
     */
    public static String encodePathSegments(String... segs) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < segs.length; i++) {
            if (i > 0) {
                sb.append('/');
            }

            sb.append(encodePathSegment(segs[i]));
        }

        return sb.toString();
    }
}
