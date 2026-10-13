package rosa.archive.iiif;

import static rosa.archive.iiif.AnnotationHtml.blank;
import static rosa.archive.iiif.AnnotationHtml.clean;
import static rosa.archive.iiif.AnnotationHtml.esc;
import static rosa.archive.iiif.AnnotationHtml.humanize;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Converts a TEI verse transcription (as used by the Roman de la Rose) into annotation
 * body HTML, split by page ({@code <pb n>}) and column ({@code <cb n>}).
 *
 * <p>Repeated page breaks with the same {@code n} continue the same page. Each column
 * becomes one HTML fragment headed "Column a". Within a column:
 *
 * <ul>
 *   <li>a line group ({@code lg}) is one paragraph, each line ({@code l}) a {@code span}
 *       separated by {@code br}</li>
 *   <li>every 5th logical line number ({@code l/@n}) is shown as {@code sub} at the start
 *       of the line, and every 5th Lecoy number ({@code milestone[@ed='lecoy']}) as
 *       {@code sup} at the end</li>
 *   <li>{@code hi rend="init"} is {@code b}, {@code hi rend="rubric"} is {@code b i},
 *       {@code expan} is {@code i}, {@code add} is ⸌…⸍, {@code del} is ⟦…⟧</li>
 *   <li>{@code figure} becomes a paragraph with its type, head and character names</li>
 *   <li>editorial notes become {@code <small>*&nbsp;note</small>} at the point they occur</li>
 * </ul>
 */
public final class TeiTranscriptionHtml {

    /**
     * One column of a transcribed page.
     *
     * @param name the column name from {@code cb/@n}, or null when the page has no columns
     * @param html the body HTML
     */
    public record Column(String name, String html) {}

    /** Every n-th line number is displayed. */
    static final int LINE_NUMBER_INTERVAL = 5;

    private static final Pattern LEADING_DIGITS = Pattern.compile("^(\\d+)");

    private TeiTranscriptionHtml() {
    }

    /**
     * Converts a whole transcription document.
     *
     * @param xml the TEI XML
     * @return the columns of each page, keyed by {@code pb/@n} in document order
     * @throws IllegalArgumentException if the XML cannot be parsed
     */
    public static Map<String, List<Column>> convert(String xml) {
        Document doc = parse(xml);
        Renderer r = new Renderer();
        r.walk(doc.getDocumentElement());
        r.closeBlock();
        return r.result();
    }

    /**
     * Finds the columns for an image, trying the page names an image id may be
     * transcribed under: {@code Book.001r}, {@code 001r} and {@code 1r}.
     *
     * @param pages   the converted pages
     * @param imageId the image id, e.g. {@code Douce332.001r.tif}
     * @return the columns, or an empty list
     */
    public static List<Column> findPage(Map<String, List<Column>> pages, String imageId) {
        if (pages == null || imageId == null) {
            return List.of();
        }
        String base = imageId.endsWith(".tif") ? imageId.substring(0, imageId.length() - 4) : imageId;
        List<String> candidates = new ArrayList<>();
        candidates.add(base);
        int dot = base.indexOf('.');
        if (dot > 0) {
            String shortName = base.substring(dot + 1);
            candidates.add(shortName);
            candidates.add(shortName.replaceFirst("^0+(?=\\d)", ""));
        }
        for (String c : candidates) {
            List<Column> cols = pages.get(c);
            if (cols != null) {
                return cols;
            }
        }
        return List.of();
    }

    private static Document parse(String xml) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setExpandEntityReferences(false);
            DocumentBuilder b = f.newDocumentBuilder();
            b.setErrorHandler(null);
            return b.parse(new InputSource(new StringReader(xml)));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalArgumentException("Cannot parse transcription XML: " + e.getMessage(), e);
        }
    }

    static boolean showNumber(String n) {
        if (blank(n)) {
            return false;
        }
        Matcher m = LEADING_DIGITS.matcher(n.trim());
        if (!m.find()) {
            return false;
        }
        try {
            int v = Integer.parseInt(m.group(1));
            return v > 0 && v % LINE_NUMBER_INTERVAL == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    static String noteLabel(String type) {
        if (blank(type)) {
            return "";
        }
        return switch (type) {
            case "critical" -> "";
            case "correction" -> "Correction: ";
            case "order" -> "Reading order: ";
            case "scribalAnnotation" -> "Scribal annotation: ";
            default -> humanize(type) + ": ";
        };
    }

    private static String name(Node n) {
        String local = n.getLocalName();
        return local != null ? local : n.getNodeName();
    }

    /** A column being built. */
    private static final class Bucket {
        final StringBuilder html = new StringBuilder();
        boolean blockOpen;
        boolean blockHasContent;
    }

    private static final class Renderer {
        private final Map<String, Map<String, Bucket>> pages = new LinkedHashMap<>();
        private String page;
        private String column = "";

        /** The current line, or null outside {@code l}. */
        private StringBuilder line;
        private String lineNumber;
        private String lecoyNumber;

        Map<String, List<Column>> result() {
            Map<String, List<Column>> out = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, Bucket>> p : pages.entrySet()) {
                List<Column> cols = new ArrayList<>();
                for (Map.Entry<String, Bucket> c : p.getValue().entrySet()) {
                    String body = c.getValue().html.toString();
                    if (body.isBlank()) {
                        continue;
                    }
                    String colName = c.getKey().isEmpty() ? null : c.getKey();
                    String html = colName == null ? body
                            : "<p><b>Column " + esc(colName) + "</b></p>" + body;
                    cols.add(new Column(colName, html));
                }
                if (!cols.isEmpty()) {
                    out.put(p.getKey(), cols);
                }
            }
            return out;
        }

        // ---- Output ----

        private Bucket bucket() {
            if (page == null) {
                return null;
            }
            return pages.computeIfAbsent(page, _ -> new LinkedHashMap<>())
                    .computeIfAbsent(column, _ -> new Bucket());
        }

        /** Ensures a paragraph is open in the current column and returns it. */
        private Bucket openBlock() {
            Bucket b = bucket();
            if (b != null && !b.blockOpen) {
                b.html.append("<p>");
                b.blockOpen = true;
                b.blockHasContent = false;
            }
            return b;
        }

        void closeBlock() {
            Bucket b = bucket();
            if (b != null && b.blockOpen) {
                if (b.blockHasContent) {
                    b.html.append("</p>");
                } else {
                    b.html.setLength(b.html.length() - 3);
                }
                b.blockOpen = false;
            }
        }

        /** Appends inline HTML to the current line, or to an open paragraph outside lines. */
        private void inline(String html) {
            if (line != null) {
                line.append(html);
                return;
            }
            Bucket b = openBlock();
            if (b != null) {
                b.html.append(html);
                b.blockHasContent = true;
            }
        }

        /** Appends a complete paragraph. */
        private void block(String html) {
            closeBlock();
            Bucket b = bucket();
            if (b != null && !html.isEmpty()) {
                b.html.append(html);
            }
        }

        // ---- Tree walk ----

        void walk(Node node) {
            for (Node c = node.getFirstChild(); c != null; c = c.getNextSibling()) {
                switch (c.getNodeType()) {
                    case Node.TEXT_NODE, Node.CDATA_SECTION_NODE -> text(c.getNodeValue());
                    case Node.ELEMENT_NODE -> element((Element) c);
                    default -> {
                    }
                }
            }
        }

        private void text(String value) {
            if (value == null) {
                return;
            }
            if (line != null) {
                line.append(esc(value));
            } else if (!value.isBlank()) {
                inline(esc(value));
            } else {
                Bucket b = bucket();
                if (b != null && b.blockOpen && b.blockHasContent
                        && !endsWith(b.html, "</span>") && !endsWith(b.html, " ")) {
                    b.html.append(' ');
                }
            }
        }

        private static boolean endsWith(StringBuilder sb, String suffix) {
            int n = sb.length() - suffix.length();
            return n >= 0 && sb.indexOf(suffix, n) == n;
        }

        private void element(Element el) {
            switch (name(el)) {
                case "teiHeader", "desc" -> {
                }
                case "pb" -> {
                    closeBlock();
                    String n = el.getAttribute("n");
                    page = blank(n) ? page : n.trim();
                    column = "";
                }
                case "cb" -> {
                    closeBlock();
                    column = clean(el.getAttribute("n"));
                }
                case "lg" -> {
                    if (line == null) {
                        closeBlock();
                        walk(el);
                        closeBlock();
                    } else {
                        walk(el);
                    }
                }
                case "l" -> lineElement(el);
                case "milestone" -> {
                    if ("lecoy".equals(el.getAttribute("ed")) && line != null) {
                        lecoyNumber = el.getAttribute("n");
                    }
                }
                case "hi" -> hi(el);
                case "expan" -> wrap(el, "<i>", "</i>");
                case "add" -> wrap(el, "\u2e0c", "\u2e0d");
                case "del" -> wrap(el, "\u27e6", "\u27e7");
                case "note" -> note(el);
                case "figure" -> figure(el);
                case "head" -> {
                    if (line != null) {
                        wrap(el, "<b>", "</b>");
                    } else {
                        String t = textOf(el);
                        if (!t.isEmpty()) block("<p><b>" + esc(t) + "</b></p>");
                    }
                }
                case "gap" -> {
                    String desc = textOf(el);
                    String t = desc.isEmpty() ? "[Gap]" : "[Gap: " + desc + "]";
                    if (line != null) {
                        line.append(esc(t));
                    } else {
                        block("<p><small>" + esc(t) + "</small></p>");
                    }
                }
                case "fw" -> {
                    String t = textOf(el);
                    if (!t.isEmpty() && line == null) {
                        String type = el.getAttribute("type");
                        String label = "catch".equals(type) ? "Catchword" : blank(type) ? "Forme work" : humanize(type);
                        block("<p><small><b>" + esc(label) + ":</b> " + esc(t) + "</small></p>");
                    }
                }
                default -> walk(el);
            }
        }

        private void lineElement(Element el) {
            if (line != null) {
                // Nested line: treat as inline content
                walk(el);
                return;
            }
            line = new StringBuilder();
            lineNumber = el.getAttribute("n");
            lecoyNumber = null;
            walk(el);
            String content = clean(line.toString());
            line = null;

            StringBuilder span = new StringBuilder("<span>");
            if (showNumber(lineNumber)) {
                span.append("<sub>").append(esc(lineNumber.trim())).append("</sub> ");
            }
            span.append(content);
            if (showNumber(lecoyNumber)) {
                span.append(" <sup>").append(esc(lecoyNumber.trim())).append("</sup>");
            }
            span.append("</span>");

            Bucket b = openBlock();
            if (b != null) {
                if (b.blockHasContent) {
                    b.html.append("<br>");
                }
                b.html.append(span);
                b.blockHasContent = true;
            }
        }

        private void hi(Element el) {
            String rend = el.getAttribute("rend");
            if ("rubric".equals(rend)) {
                wrap(el, "<b><i>", "</i></b>");
            } else if (rend.startsWith("init")) {
                wrap(el, "<b>", "</b>");
            } else {
                walk(el);
            }
        }

        private void wrap(Element el, String open, String close) {
            inline(open);
            walk(el);
            inline(close);
        }

        private void note(Element el) {
            String type = el.getAttribute("type");
            String t = textOf(el);
            if (t.isEmpty()) {
                return;
            }
            if ("scribalPunc".equals(type)) {
                inline(esc(t));
            } else if ("character".equals(type)) {
                inline(" <small>" + esc(t) + "</small> ");
            } else {
                inline("<small>*&nbsp;" + esc(noteLabel(type) + t) + "</small>");
            }
        }

        private void figure(Element el) {
            List<String> heads = new ArrayList<>();
            List<String> characters = new ArrayList<>();
            List<String> notes = new ArrayList<>();
            for (Node c = el.getFirstChild(); c != null; c = c.getNextSibling()) {
                if (c.getNodeType() != Node.ELEMENT_NODE) continue;
                Element child = (Element) c;
                String t = textOf(child);
                if (t.isEmpty()) continue;
                switch (name(child)) {
                    case "head" -> heads.add(t);
                    case "note" -> {
                        if ("character".equals(child.getAttribute("type"))) {
                            if (!characters.contains(t)) characters.add(t);
                        } else {
                            notes.add("<small>*&nbsp;" + esc(noteLabel(child.getAttribute("type")) + t) + "</small>");
                        }
                    }
                    default -> heads.add(t);
                }
            }
            String type = el.getAttribute("type");
            String label = blank(type) ? "Figure" : humanize(type);
            StringBuilder html = new StringBuilder("<p><b>").append(esc(label)).append(":</b>");
            if (!heads.isEmpty()) {
                html.append(" <i>").append(esc(String.join(" ", heads))).append("</i>");
            }
            for (String n : notes) {
                html.append(n);
            }
            if (!characters.isEmpty()) {
                List<String> escaped = new ArrayList<>();
                for (String ch : characters) escaped.add(esc(ch));
                html.append("<br><small><b>Characters:</b> ").append(String.join(", ", escaped)).append("</small>");
            }
            html.append("</p>");
            if (line != null) {
                // A figure inside a line is rare; keep it inline
                line.append(html.substring(3, html.length() - 4));
            } else {
                block(html.toString());
            }
        }

        private static String textOf(Element el) {
            return clean(el.getTextContent());
        }
    }
}
