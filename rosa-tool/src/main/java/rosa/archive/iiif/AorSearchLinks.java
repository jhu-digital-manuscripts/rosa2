package rosa.archive.iiif;

import java.nio.charset.StandardCharsets;

/**
 * Builds links from the names in Archaeology of Reading annotations (people, books,
 * locations) to the viewer's advanced search for that name.
 *
 * <p>The link is the viewer site's own advanced search URL, so it works as a plain link
 * in any IIIF viewer and the rosa viewer can recognise and follow it in place:
 *
 * <pre>{viewerUrl}#{collection}/search?q=people:'Plutarch'&amp;type=advanced</pre>
 *
 * <p>The value escaping (backslash before {@code \} and {@code '}) and the parameter
 * encoding (encodeURIComponent with {@code +} for spaces and {@code : , /} kept readable)
 * mirror {@code rosa-viewer/src/site/history/advancedQuery.js} and {@code route.js}; the
 * two must stay in step for the viewer to parse the fragment.
 *
 * @param viewerUrl    the URL of the viewer site the fragment is appended to, e.g.
 *                     {@code https://example.org/aor/}
 * @param collectionId the collection the search runs in
 */
public record AorSearchLinks(String viewerUrl, String collectionId) {

    /** Characters encodeURIComponent leaves as they are. */
    private static final String UNRESERVED = "-_.!~*'()";

    /** Characters route.js leaves readable after encoding. */
    private static final String READABLE = ":,/";

    public AorSearchLinks {
        if (viewerUrl == null || viewerUrl.isBlank()) {
            throw new IllegalArgumentException("viewerUrl is required");
        }
        if (collectionId == null || collectionId.isBlank()) {
            throw new IllegalArgumentException("collectionId is required");
        }
        viewerUrl = viewerUrl.trim();
        collectionId = collectionId.trim();
    }

    /**
     * The advanced search URL for one field value, or null when the value is blank.
     *
     * @param field the search field, e.g. {@code people}
     * @param value the value, e.g. a person's name
     * @return the URL, not HTML-escaped
     */
    public String href(String field, String value) {
        String v = AnnotationHtml.clean(value);
        if (v.isEmpty()) {
            return null;
        }
        String term = field + ":'" + escapeValue(v) + "'";
        return viewerUrl + "#" + encodeURIComponent(collectionId) + "/search?q=" + encodeParam(term) + "&type=advanced";
    }

    /**
     * Escapes a value for the advanced query grammar: {@code \} and {@code '} get a backslash.
     */
    static String escapeValue(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    /**
     * Encodes a fragment parameter the way route.js does: encodeURIComponent, then
     * {@code %20} becomes {@code +} and {@code : , /} are left readable.
     */
    static String encodeParam(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 16);
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            char c = (char) (b & 0xff);
            if (isUnreserved(c) || READABLE.indexOf(c) >= 0) {
                sb.append(c);
            } else if (c == ' ') {
                sb.append('+');
            } else {
                sb.append('%').append(Character.toUpperCase(Character.forDigit((b >> 4) & 0xf, 16)))
                        .append(Character.toUpperCase(Character.forDigit(b & 0xf, 16)));
            }
        }
        return sb.toString();
    }

    /** Plain encodeURIComponent. */
    static String encodeURIComponent(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 16);
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            char c = (char) (b & 0xff);
            if (isUnreserved(c)) {
                sb.append(c);
            } else {
                sb.append('%').append(Character.toUpperCase(Character.forDigit((b >> 4) & 0xf, 16)))
                        .append(Character.toUpperCase(Character.forDigit(b & 0xf, 16)));
            }
        }
        return sb.toString();
    }

    private static boolean isUnreserved(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                || UNRESERVED.indexOf(c) >= 0;
    }
}
