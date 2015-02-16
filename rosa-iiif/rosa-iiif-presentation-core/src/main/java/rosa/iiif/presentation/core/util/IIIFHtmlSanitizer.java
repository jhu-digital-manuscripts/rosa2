package rosa.iiif.presentation.core.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class IIIFHtmlSanitizer {
    private Set<String> allowedTags;
    private Map<String, Set<String>> allowedAttributes;

    private static final String HTML_ENTITY_REGEX = "[a-z]+|#[0-9]+|#x[0-9a-fA-F]+";

    private IIIFHtmlSanitizer() {
        allowedTags = new HashSet<>();
        allowedAttributes = new HashMap<>();
    }

    /**
     * @return a fresh IIIFHtmlSanitizer that, by default, recognizes no
     *      tags or attributes
     */
    public static IIIFHtmlSanitizer newSanitizer() {
        return new IIIFHtmlSanitizer();
    }

    /**
     * @return
     *          a new IIIFHtmlSanitizer that recognizes all tags and attributes
     *          allowed under the IIIF Presentation API.
     */
    public static IIIFHtmlSanitizer defaultSanitizer() {
        return new IIIFHtmlSanitizer()
                .addTags("a", "b", "br", "i", "img", "p", "span")
                .addAttributes("a", "href")
                .addAttributes("img", "src", "alt");
    }

    /**
     * Sanitize an input string by keeping only those HTML tags and attributes
     * specified, escaping all others
     *
     * @param html input string containing arbitrary HTML
     * @return cleaned HTML
     */
    public String sanitize(String html) {
        if (html == null) {
            throw new IllegalArgumentException("HTML cannot be NULL");
        }
        return simpleSanitize(html);
    }

    /**
     * Add one or more tags to this IIIFHtmlSanitizer to be recognized as
     * valid tags. These tags will be allowed any sanitized output.
     *
     * @param tags one or more tags to be recognized as valid
     * @return the updated IIIFHtmlSanitizer
     */
    public IIIFHtmlSanitizer addTags(String ... tags) {
        if (tags == null || tags.length == 0) {
            throw new IllegalArgumentException("No tags specified.");
        }
        allowedTags.addAll(Arrays.asList(tags));
        return this;
    }

    /**
     * Add one or more attributes associated with an HTML tag to be seen as
     * valid. Any attribute not added to this sanitizer will be seen as
     * invalid and will be removed in sanitized output.
     *
     * @param tag HTML tag
     * @param attributes one or more attributes
     * @return the updated IIIFHtmlSanitizer
     */
    public IIIFHtmlSanitizer addAttributes(String tag, String ... attributes) {
        if (tag == null || attributes == null) {
            throw new IllegalArgumentException("Cannot have NULL tag or attributes.");
        }
        if (attributes.length == 0) {
            throw new IllegalArgumentException("No attributes specified.");
        }

        if (!allowedAttributes.containsKey(tag)) {
            allowedAttributes.put(tag, new HashSet<String>());
        }
        allowedAttributes.get(tag).addAll(Arrays.asList(attributes));
        return this;
    }

    /*
     * Sanitize a string containing simple HTML markup as defined above. The
     * approach is as follows: We split the string at each occurrence of '<'. Each
     * segment thus obtained is inspected to determine if the leading '<' was
     * indeed the start of a whitelisted tag or not. If so, the tag is emitted
     * unescaped, and the remainder of the segment (which cannot contain any
     * additional tags) is emitted in escaped form. Otherwise, the entire segment
     * is emitted in escaped form.
     *
     * In either case, EscapeUtils.htmlEscapeAllowEntities is used to escape,
     * which escapes HTML but does not double escape existing syntactically valid
     * HTML entities.
     */
    private String simpleSanitize(String text) {
        StringBuilder sanitized = new StringBuilder();

        boolean firstSegment = true;
        for (String segment : text.split("<", -1)) {
            if (firstSegment) {
            /*
             *  the first segment is never part of a valid tag; note that if the
             *  input string starts with a tag, we will get an empty segment at the
             *  beginning.
             */
                firstSegment = false;
                sanitized.append(htmlEscapeAllowEntities(segment));
                continue;
            }

          /*
           *  determine if the current segment is the start of an attribute-free tag
           *  or end-tag in our whitelist
           */
            int tagStart = 0; // will be 1 if this turns out to be an end tag.
            int tagEnd = segment.indexOf('>');
            int tagNameEnd = segment.indexOf(" ");
            boolean hasAttributes = tagNameEnd > 0 && tagNameEnd < tagEnd;
            boolean isSelfClosing = segment.substring(tagEnd - 1, tagEnd).equals("/");

            if (tagNameEnd == -1 || tagNameEnd > tagEnd) {
                tagNameEnd = tagEnd;
            }

            String tag = null;
            Map<String, String> attrMap = null;

            boolean isValidTag = false;
            if (tagEnd > 0) {
                if (segment.charAt(0) == '/') {
                    tagStart = 1;
                }
                tag = segment.substring(tagStart, tagNameEnd);
                if (hasAttributes) {
                    attrMap = getAttributes(segment.substring(tagNameEnd, tagEnd));
                }
                isValidTag = allowedTags.contains(tag);
            }

            if (isValidTag) {
                // append the tag, not escaping it
                if (tagStart == 0) {
                    sanitized.append('<').append(tag);
                    if (attrMap != null) {
                        for (String attr : attrMap.keySet()) {
                            if (isAllowed(tag, attr)) {
                                String val = attrMap.get(attr);
                                sanitized.append(' ').append(attr).append("=\"").append(val).append('"');
                            }
                        }
                    }
                } else {
                    // we had seen an end-tag
                    sanitized.append("</").append(tag);
                }
                if (isSelfClosing) {
                    sanitized.append('/');
                }
                sanitized.append('>');

                // append the rest of the segment, escaping it
                sanitized.append(htmlEscapeAllowEntities(segment.substring(tagEnd + 1)));
            } else {
                // just escape the whole segment
                sanitized.append("&lt;").append(htmlEscapeAllowEntities(segment));
            }
        }
        return sanitized.toString();
    }

    /*
     * Take a string of attributes, and put them in a map.
     *
     * If a tag looks like: &lt;tagname attr1="val1" attr2="val2">
     * the attribute string is 'attr1="val1" attr2="val2"'
     *
     * The attribute string is first split on '" ' (double quote + space). Each of
     * these parts are then split on '=' (equals sign). For all valid attributes,
     * this will result in a two element array: { attr_name, attr_val }. Attr_val
     * is stripped of double quotes at either end, if necessary.
     *
     * @param attr attribute string
     * @return map of attributes
     */
    private Map<String, String> getAttributes(String attr) {
        if (attr == null) {
            return null;
        }

        Map<String, String> attrs = new HashMap<>();
        if (attr.endsWith("/")) {
            attr = attr.substring(0, attr.length() - 1);
        }

        String[] parts = attr.split("\"\\s");
        for (String part : parts) {
            String[] av_pair = part.split("=");
            if (av_pair.length != 2) {
                continue;
            }

            if (av_pair[1].startsWith("\"")) {
                av_pair[1] = av_pair[1].substring(1);
            }
            if (av_pair[1].endsWith("\"")) {
                av_pair[1] = av_pair[1].substring(0, av_pair[1].length() - 1);
            }

            attrs.put(av_pair[0].trim(), av_pair[1].trim());
        }

        return attrs;
    }

    private boolean isAllowed(String tag, String attribute) {
        return allowedAttributes.containsKey(tag) && allowedAttributes.get(tag).contains(attribute);
    }

    private String htmlEscapeAllowEntities(String text) {
        StringBuilder escaped = new StringBuilder();

        boolean firstSegment = true;
        for (String segment : text.split("&", -1)) {
            if (firstSegment) {
        /*
         * The first segment is never part of an entity reference, so we always
         * escape it.
         * Note that if the input starts with an ampersand, we will get an empty
         * segment before that.
         */
                firstSegment = false;

                escaped.append(htmlEscape(segment));
                continue;
            }

            int entityEnd = segment.indexOf(';');
            if (entityEnd > 0 && segment.substring(0, entityEnd).matches(HTML_ENTITY_REGEX)) {
                // Append the entity without escaping.
                escaped.append("&").append(segment.substring(0, entityEnd + 1));
                // Append the rest of the segment, escaped.
                escaped.append(htmlEscape(segment.substring(entityEnd + 1)));
            } else {
                // The segment did not start with an entity reference, so escape the
                // whole segment.
                escaped.append("&amp;").append(htmlEscape(segment));
            }
        }

        return escaped.toString();
    }

    private String htmlEscape(String s) {
        if (s.contains("&")) {
            s = s.replaceAll("&", "&amp;");
        }
        if (s.contains("<")) {
            s = s.replaceAll("<", "&lt;");
        }
        if (s.contains(">")) {
            s = s.replaceAll(">", "&gt;");
        }
        if (s.contains("\"")) {
            s = s.replaceAll("\"", "&quot;");
        }
        if (s.contains("'")) {
            s = s.replaceAll("'", "&#39;");
        }
        return s;
    }

}
