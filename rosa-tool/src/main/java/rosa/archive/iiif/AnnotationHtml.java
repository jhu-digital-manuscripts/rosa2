package rosa.archive.iiif;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helpers for building annotation body HTML that follows the IIIF cookbook rules for
 * HTML in annotations (recipe 0019).
 *
 * <p>Bodies use only the tags {@link #ALLOWED_TAGS}, with {@code href} on {@code a} and
 * {@code src}/{@code alt} on {@code img}. They are well-formed and start with {@code <}
 * and end with {@code >}. Structure follows a small set of conventions that the viewer
 * stylesheets rely on:
 *
 * <ul>
 *   <li>the first paragraph is a heading: {@code <p><b>Label</b> detail <small>extra</small></p>}</li>
 *   <li>translations are {@code <p><i>…</i></p>}</li>
 *   <li>metadata lines are {@code <p><small><b>Label:</b> …</small></p>}</li>
 * </ul>
 */
public final class AnnotationHtml {

    /** HTML tags allowed in annotation bodies. */
    public static final Set<String> ALLOWED_TAGS =
            Set.of("a", "b", "br", "i", "img", "p", "small", "span", "sub", "sup");

    private static final Pattern TAG = Pattern.compile("<(/?)([a-zA-Z0-9]+)([^>]*?)(/?)>");
    private static final Pattern ATTR = Pattern.compile("([a-zA-Z_:][-a-zA-Z0-9_:.]*)\\s*=\\s*\"[^\"]*\"");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private AnnotationHtml() {
    }

    /**
     * Escapes text for inclusion in HTML element content or attribute values.
     *
     * @param text the text, may be null
     * @return the escaped text, never null
     */
    public static String esc(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Collapses runs of whitespace to single spaces and trims.
     *
     * @param text the text, may be null
     * @return the normalized text, never null
     */
    public static String clean(String text) {
        if (text == null) {
            return "";
        }
        return WHITESPACE.matcher(text).replaceAll(" ").trim();
    }

    /**
     * Returns true when the text is null or blank.
     */
    public static boolean blank(String text) {
        return text == null || text.isBlank();
    }

    /**
     * Turns an archive name such as {@code quotation_mark} or {@code Bisected_circle}
     * into a display label ({@code Quotation mark}, {@code Bisected circle}).
     *
     * @param name the name, may be null
     * @return the label, or an empty string
     */
    public static String humanize(String name) {
        String s = clean(name == null ? null : name.replace('_', ' '));
        if (s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Builds a heading paragraph: bold label, optional plain detail and optional small extra.
     *
     * @param label  the label (escaped here)
     * @param detail the detail, or null (escaped here)
     * @param extra  the small extra text, or null (escaped here)
     * @return the paragraph HTML
     */
    public static String heading(String label, String detail, String extra) {
        StringBuilder sb = new StringBuilder("<p><b>").append(esc(clean(label))).append("</b>");
        if (!blank(detail)) {
            sb.append(' ').append(esc(clean(detail)));
        }
        if (!blank(extra)) {
            sb.append(" <small>").append(esc(clean(extra))).append("</small>");
        }
        return sb.append("</p>").toString();
    }

    /**
     * Builds a paragraph of escaped text, or an empty string if the text is blank.
     */
    public static String para(String text) {
        return blank(text) ? "" : "<p>" + esc(clean(text)) + "</p>";
    }

    /**
     * Builds a paragraph of quoted, escaped text, or an empty string if the text is blank.
     */
    public static String quoted(String text) {
        return blank(text) ? "" : "<p>\u201c" + esc(clean(text)) + "\u201d</p>";
    }

    /**
     * Builds a translation paragraph, or an empty string if the text is blank.
     */
    public static String translation(String text) {
        return blank(text) ? "" : "<p><i>" + esc(clean(text)) + "</i></p>";
    }

    /**
     * Builds a metadata line {@code <p><small><b>Label:</b> v1, v2</small></p>}.
     * Blank and duplicate values are dropped; returns an empty string if none remain.
     *
     * @param label  the label, without a colon
     * @param values the values
     * @return the paragraph HTML, or an empty string
     */
    public static String meta(String label, Collection<String> values) {
        List<String> vals = distinct(values);
        if (vals.isEmpty()) {
            return "";
        }
        List<String> escaped = new ArrayList<>(vals.size());
        for (String v : vals) {
            escaped.add(esc(v));
        }
        return "<p><small><b>" + esc(label) + ":</b> " + String.join(", ", escaped) + "</small></p>";
    }

    /**
     * Builds a metadata line with a single value.
     */
    public static String meta(String label, String value) {
        return blank(value) ? "" : meta(label, List.of(value));
    }

    /**
     * Joins the non-blank parts with a separator.
     */
    public static String join(String separator, String... parts) {
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            if (!blank(p)) {
                out.add(clean(p));
            }
        }
        return String.join(separator, out);
    }

    /**
     * Returns the cleaned, non-blank values in order with duplicates removed.
     */
    public static List<String> distinct(Collection<String> values) {
        List<String> out = new ArrayList<>();
        if (values == null) {
            return out;
        }
        for (String v : values) {
            String c = clean(v);
            if (!c.isEmpty() && !out.contains(c)) {
                out.add(c);
            }
        }
        return out;
    }

    /**
     * Normalizes a language code for an annotation body: lowercased, or null when blank
     * or the IIIF "none" value.
     *
     * @param lang the language code, may be null
     * @return the normalized code, or null
     */
    public static String normalizeLanguage(String lang) {
        if (blank(lang)) {
            return null;
        }
        String l = lang.trim().toLowerCase(Locale.ROOT);
        return "none".equals(l) ? null : l;
    }

    /**
     * Returns a display name for an AoR language code such as {@code LA}.
     *
     * @param code the language code, may be null
     * @return the language name, the code itself if unknown, or null when blank
     */
    public static String languageName(String code) {
        if (blank(code)) {
            return null;
        }
        return switch (code.trim().toLowerCase(Locale.ROOT)) {
            case "la" -> "Latin";
            case "it" -> "Italian";
            case "en" -> "English";
            case "fr" -> "French";
            case "el", "gr", "grc" -> "Greek";
            case "es" -> "Spanish";
            case "de" -> "German";
            case "he" -> "Hebrew";
            case "nl" -> "Dutch";
            default -> code.trim();
        };
    }

    /**
     * Checks that HTML follows the annotation rules: only allowed tags and attributes,
     * balanced tags, and the first and last characters are {@code <} and {@code >}.
     *
     * @param html the HTML
     * @return a description of the first problem found, or null if the HTML is compliant
     */
    public static String findViolation(String html) {
        if (html == null || html.isEmpty()) {
            return "empty";
        }
        if (html.charAt(0) != '<' || html.charAt(html.length() - 1) != '>') {
            return "must start with < and end with >";
        }
        List<String> stack = new ArrayList<>();
        Matcher m = TAG.matcher(html);
        while (m.find()) {
            boolean closing = !m.group(1).isEmpty();
            String tag = m.group(2).toLowerCase(Locale.ROOT);
            String attrs = m.group(3);
            boolean selfClosing = !m.group(4).isEmpty() || tag.equals("br") || tag.equals("img");
            if (!ALLOWED_TAGS.contains(tag)) {
                return "tag not allowed: " + tag;
            }
            Matcher am = ATTR.matcher(attrs);
            String rest = am.replaceAll("").trim();
            if (!rest.isEmpty()) {
                return "malformed attributes on " + tag + ": " + attrs;
            }
            am.reset();
            while (am.find()) {
                String attr = am.group(1).toLowerCase(Locale.ROOT);
                boolean ok = (tag.equals("a") && attr.equals("href"))
                        || (tag.equals("img") && (attr.equals("src") || attr.equals("alt")));
                if (!ok) {
                    return "attribute not allowed: " + tag + "@" + attr;
                }
            }
            if (closing) {
                if (stack.isEmpty() || !Objects.equals(stack.removeLast(), tag)) {
                    return "unbalanced closing tag: " + tag;
                }
            } else if (!selfClosing) {
                stack.add(tag);
            }
        }
        return stack.isEmpty() ? null : "unclosed tag: " + stack.getLast();
    }
}
