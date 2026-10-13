package rosa.archive.iiif;

import static rosa.archive.iiif.AnnotationHtml.blank;
import static rosa.archive.iiif.AnnotationHtml.clean;
import static rosa.archive.iiif.AnnotationHtml.esc;
import static rosa.archive.iiif.AnnotationHtml.heading;
import static rosa.archive.iiif.AnnotationHtml.humanize;
import static rosa.archive.iiif.AnnotationHtml.join;
import static rosa.archive.iiif.AnnotationHtml.languageName;
import static rosa.archive.iiif.AnnotationHtml.meta;
import static rosa.archive.iiif.AnnotationHtml.normalizeLanguage;
import static rosa.archive.iiif.AnnotationHtml.para;
import static rosa.archive.iiif.AnnotationHtml.quoted;
import static rosa.archive.iiif.AnnotationHtml.translation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.AnnotationLink;
import rosa.archive.model.aor.Calculation;
import rosa.archive.model.aor.Drawing;
import rosa.archive.model.aor.Errata;
import rosa.archive.model.aor.Graph;
import rosa.archive.model.aor.GraphNode;
import rosa.archive.model.aor.GraphNote;
import rosa.archive.model.aor.GraphText;
import rosa.archive.model.aor.InternalReference;
import rosa.archive.model.aor.Location;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.MarginaliaLanguage;
import rosa.archive.model.aor.Mark;
import rosa.archive.model.aor.Numeral;
import rosa.archive.model.aor.PhysicalLink;
import rosa.archive.model.aor.Position;
import rosa.archive.model.aor.ReferenceTarget;
import rosa.archive.model.aor.Symbol;
import rosa.archive.model.aor.Table;
import rosa.archive.model.aor.TableCell;
import rosa.archive.model.aor.TableHeader;
import rosa.archive.model.aor.TextEl;
import rosa.archive.model.aor.Underline;
import rosa.archive.model.aor.XRef;

/**
 * Formats the Archaeology of Reading annotations on a page as annotation bodies.
 *
 * <p>Produces one item per reader's intervention, plus a leading page header item
 * ({@code aor-page}) with pagination, signature and reader. Items carry the classes
 * {@code rosa-anno aor-{type} aor-place-{place}} and are ordered by place
 * (head, left margin, in text, right margin, tail, full page, then unplaced),
 * keeping a fixed type order within each place.
 */
public final class AorAnnotationFormatter {

    /** Display order of places on the page. */
    public static final List<Location> PLACE_ORDER = List.of(
            Location.HEAD, Location.LEFT_MARGIN, Location.INTEXT,
            Location.RIGHT_MARGIN, Location.TAIL, Location.FULL_PAGE);

    private record Placed(Location place, AnnotationItem item) {}

    private AorAnnotationFormatter() {
    }

    /**
     * Formats all annotations on a page.
     *
     * @param page the annotated page
     * @return the items: the page header first (if any), then annotations in place order
     */
    public static List<AnnotationItem> format(AnnotatedPage page) {
        List<Placed> placed = new ArrayList<>();
        for (Marginalia m : page.getMarginalia()) add(placed, marginalia(m));
        for (Drawing d : page.getDrawings()) add(placed, drawing(d));
        for (Graph g : page.getGraphs()) add(placed, graph(g));
        for (Table t : page.getTables()) add(placed, table(t));
        for (Calculation c : page.getCalculations()) add(placed, calculation(c));
        for (Symbol s : page.getSymbols()) add(placed, symbol(s));
        for (Numeral n : page.getNumerals()) add(placed, numeral(n));
        for (Mark m : page.getMarks()) add(placed, mark(m));
        for (Underline u : page.getUnderlines()) add(placed, underline(u));
        for (Errata e : page.getErrata()) add(placed, errata(e));
        for (PhysicalLink l : page.getLinks()) add(placed, link(l));

        // Stable sort keeps the type order within each place
        placed.sort(Comparator.comparingInt(p -> placeRank(p.place())));

        List<AnnotationItem> items = new ArrayList<>();
        AnnotationItem header = pageHeader(page);
        if (header != null) {
            items.add(header);
        }
        for (Placed p : placed) {
            items.add(p.item());
        }
        return items;
    }

    /**
     * Returns the class name suffix for a place, e.g. {@code left_margin}.
     */
    public static String placeKey(Location place) {
        return place.name().toLowerCase(Locale.ROOT);
    }

    /**
     * Returns a display label for a place, e.g. "Left margin".
     */
    public static String placeLabel(Location place) {
        return switch (place) {
            case HEAD -> "Head";
            case TAIL -> "Tail";
            case LEFT_MARGIN -> "Left margin";
            case RIGHT_MARGIN -> "Right margin";
            case INTEXT -> "In text";
            case FULL_PAGE -> "Full page";
        };
    }

    private static int placeRank(Location place) {
        int i = place == null ? -1 : PLACE_ORDER.indexOf(place);
        return i < 0 ? PLACE_ORDER.size() : i;
    }

    private static void add(List<Placed> out, Placed p) {
        if (p != null) {
            out.add(p);
        }
    }

    private static Placed placed(String type, Location place, StringBuilder html, String lang) {
        if (html.isEmpty()) {
            return null;
        }
        List<String> classes = new ArrayList<>(List.of("rosa-anno", "aor-" + type));
        if (place != null) {
            classes.add("aor-place-" + placeKey(place));
        }
        return new Placed(place, new AnnotationItem(classes, html.toString(), normalizeLanguage(lang)));
    }

    // ---- Page header ----

    private static AnnotationItem pageHeader(AnnotatedPage page) {
        String pagination = clean(page.getPagination());
        String signature = clean(page.getSignature());
        String reader = clean(page.getReader());
        if (pagination.isEmpty() && signature.isEmpty() && reader.isEmpty()) {
            return null;
        }
        StringBuilder html = new StringBuilder();
        String title = !pagination.isEmpty() ? pagination : signature;
        if (!title.isEmpty()) {
            String extra = !pagination.isEmpty() && !signature.isEmpty() ? "Signature " + signature : null;
            html.append(heading(title, null, extra));
        }
        html.append(meta("Reader", reader));
        return new AnnotationItem(List.of("rosa-anno", "aor-page"), html.toString(), "en");
    }

    // ---- Marginalia ----

    private static Placed marginalia(Marginalia m) {
        Location first = null;
        for (MarginaliaLanguage ml : m.getLanguages()) {
            for (Position pos : ml.getPositions()) {
                if (pos.getPlace() != null) {
                    first = pos.getPlace();
                    break;
                }
            }
            if (first != null) break;
        }
        if (first == null) {
            first = m.getLocation();
        }

        List<String> languageNames = new ArrayList<>();
        List<String> people = new ArrayList<>();
        List<String> books = new ArrayList<>();
        List<String> locations = new ArrayList<>();
        List<String> symbols = new ArrayList<>();
        List<String> emphasis = new ArrayList<>();
        List<String> xrefs = new ArrayList<>();
        List<String> refs = new ArrayList<>();
        boolean rotated = false;
        StringBuilder text = new StringBuilder();
        String firstLang = null;

        for (MarginaliaLanguage ml : m.getLanguages()) {
            String name = languageName(ml.getLang());
            if (name != null && !languageNames.contains(name)) {
                languageNames.add(name);
            }
            if (firstLang == null && !blank(ml.getLang())) {
                firstLang = ml.getLang();
            }
            for (Position pos : ml.getPositions()) {
                String body = clean(String.join(" ", pos.getTexts()));
                if (!body.isEmpty()) {
                    text.append("<p>");
                    if (pos.getPlace() != null && pos.getPlace() != first) {
                        text.append("<small>[").append(placeLabel(pos.getPlace())).append("]</small> ");
                    }
                    text.append(esc(body)).append("</p>");
                }
                if (pos.getOrientation() != 0) rotated = true;
                people.addAll(pos.getPeople());
                books.addAll(pos.getBooks());
                locations.addAll(pos.getLocations());
                symbols.addAll(pos.getSymbols());
                for (Underline u : pos.getEmphasis()) emphasis.add(u.getReferencedText());
                for (XRef x : pos.getXRefs()) xrefs.add(xref(x));
                for (InternalReference r : pos.getInternalRefs()) refs.add(internalRef(r));
                for (InternalReference r : pos.getMarginaliaRefs()) refs.add(internalRef(r));
            }
        }
        if (text.isEmpty() && blank(m.getTranslation())) {
            return null;
        }

        String hand = blank(m.getHand()) ? null : humanize(m.getHand()) + " hand";
        StringBuilder html = new StringBuilder();
        html.append(heading("Marginal note", null,
                join(", ", String.join(", ", languageNames), hand, rotated ? "book rotated" : null)));
        html.append(text);
        html.append(translation(m.getTranslation()));
        html.append(meta("Anchored to", m.getReferencedText()));
        html.append(meta("Topic", m.getTopic()));
        html.append(meta("People", people));
        html.append(meta("Books", books));
        html.append(meta("Locations", locations));
        html.append(meta("Symbols", humanizeAll(symbols)));
        html.append(meta("Underlined", emphasis));
        html.append(meta("Cross-references", xrefs));
        html.append(meta("Refers to", refs));
        html.append(meta("Other reader", m.getOtherReader()));
        html.append(meta("Date", m.getDate()));
        html.append(meta("Continues from", m.getContinuesFrom()));
        html.append(meta("Continues to", m.getContinuesTo()));
        return placed("marginalia", first, html, firstLang);
    }

    private static String xref(XRef x) {
        String who = join(", ", x.person(), x.title());
        String text = blank(x.text()) ? null : "\u201c" + clean(x.text()) + "\u201d";
        return join(": ", who, text);
    }

    private static String internalRef(InternalReference r) {
        List<String> targets = new ArrayList<>();
        for (ReferenceTarget t : r.getTargets()) {
            targets.add(join(" ", t.bookId(), t.filename(), blank(t.text()) ? null : "\u201c" + clean(t.text()) + "\u201d"));
        }
        String to = String.join("; ", AnnotationHtml.distinct(targets));
        return join(" \u2192 ", r.getText(), to);
    }

    // ---- Simple marks ----

    private static Placed underline(Underline u) {
        if (blank(u.getReferencedText())) return null;
        StringBuilder html = new StringBuilder();
        html.append(heading("Underline", null, join(", ", humanizeLower(u.getType()), u.getMethod(), u.getColor())));
        html.append(quoted(u.getReferencedText()));
        Location place = u.getLocation() != null ? u.getLocation() : Location.INTEXT;
        return placed("underline", place, html, u.getLanguage());
    }

    private static Placed mark(Mark m) {
        if (blank(m.getName()) && blank(m.getReferencedText())) return null;
        StringBuilder html = new StringBuilder();
        html.append(heading("Mark", humanize(m.getName()), join(", ", m.getMethod(), m.getColor())));
        html.append(quoted(m.getReferencedText()));
        return placed("mark", m.getLocation(), html, m.getLanguage());
    }

    private static Placed symbol(Symbol s) {
        if (blank(s.getName()) && blank(s.getReferencedText())) return null;
        StringBuilder html = new StringBuilder();
        html.append(heading("Symbol", humanize(s.getName()), null));
        html.append(quoted(s.getReferencedText()));
        return placed("symbol", s.getLocation(), html, s.getLanguage());
    }

    private static Placed numeral(Numeral n) {
        if (blank(n.getNumeral()) && blank(n.getReferencedText())) return null;
        StringBuilder html = new StringBuilder();
        html.append(heading("Numeral", n.getNumeral(), null));
        html.append(quoted(n.getReferencedText()));
        return placed("numeral", n.getLocation(), html, n.getLanguage());
    }

    private static Placed errata(Errata e) {
        String copy = clean(e.getReferencedText());
        String amended = clean(e.getAmendedText());
        if (copy.isEmpty() && amended.isEmpty()) return null;
        StringBuilder html = new StringBuilder(heading("Erratum", null, null));
        if (!copy.isEmpty() && !amended.isEmpty()) {
            html.append("<p>\u201c").append(esc(copy)).append("\u201d \u2192 \u201c")
                    .append(esc(amended)).append("\u201d</p>");
        } else {
            html.append(quoted(copy.isEmpty() ? amended : copy));
        }
        Location place = e.getLocation() != null ? e.getLocation() : Location.INTEXT;
        return placed("errata", place, html, e.getLanguage());
    }

    // ---- Complex annotations ----

    private static Placed drawing(Drawing d) {
        StringBuilder html = new StringBuilder();
        html.append(heading("Drawing", humanize(d.getType()), join(", ", d.getMethod(), d.getColor())));
        html.append(quoted(d.getReferencedText()));
        appendTexts(html, d.getTexts());
        html.append(translation(d.getTranslation()));
        html.append(meta("People", d.getPeople()));
        html.append(meta("Books", d.getBooks()));
        html.append(meta("Locations", d.getLocations()));
        html.append(meta("Symbols", humanizeAll(d.getSymbols())));
        html.append(meta("Refers to", internalRefs(d.getInternalRefs())));
        return placed("drawing", d.getLocation(), html, d.getLanguage());
    }

    private static Placed graph(Graph g) {
        StringBuilder html = new StringBuilder();
        html.append(heading("Graph", humanize(g.getType()), g.getMethod()));

        List<String> nodeLines = new ArrayList<>();
        for (GraphNode node : g.getNodes()) {
            String line = join(": ", join(" \u2014 ", node.person(), node.text()), node.content());
            if (!line.isEmpty()) nodeLines.add(esc(line));
        }
        appendLines(html, nodeLines);

        List<String> people = new ArrayList<>();
        List<String> books = new ArrayList<>();
        List<String> locations = new ArrayList<>();
        List<String> symbols = new ArrayList<>();
        for (GraphText gt : g.getGraphTexts()) {
            for (GraphNote note : gt.getNotes()) {
                html.append(para(note.content()));
            }
            for (String t : gt.getTranslations()) {
                html.append(translation(t));
            }
            people.addAll(gt.getPeople());
            books.addAll(gt.getBooks());
            locations.addAll(gt.getLocations());
            symbols.addAll(gt.getSymbols());
        }
        html.append(meta("People", people));
        html.append(meta("Books", books));
        html.append(meta("Locations", locations));
        html.append(meta("Symbols", humanizeAll(symbols)));
        html.append(meta("Refers to", internalRefs(g.getInternalRefs())));
        html.append(meta("Continues from", g.getContinuesFrom()));
        html.append(meta("Continues to", g.getContinuesTo()));
        return placed("graph", g.getLocation(), html, g.getLanguage());
    }

    private static Placed table(Table t) {
        StringBuilder html = new StringBuilder();
        html.append(heading("Table", humanize(t.getType()), null));

        List<String> headers = new ArrayList<>();
        for (TableHeader h : t.getColHeaders()) {
            String s = join(" ", h.label(), h.content());
            if (!s.isEmpty()) headers.add(s);
        }
        html.append(meta("Columns", headers));

        // Group cells by row, in column order
        Map<Integer, Map<Integer, String>> rows = new TreeMap<>();
        for (TableCell cell : t.getCells()) {
            if (!blank(cell.content())) {
                rows.computeIfAbsent(cell.row(), _ -> new TreeMap<>()).put(cell.col(), clean(cell.content()));
            }
        }
        List<String> rowLines = new ArrayList<>();
        for (Map<Integer, String> row : rows.values()) {
            rowLines.add(esc(String.join(" | ", row.values())));
        }
        appendLines(html, rowLines);

        appendTexts(html, t.getTexts());
        html.append(translation(t.getTranslation()));
        html.append(meta("People", t.getPeople()));
        html.append(meta("Books", t.getBooks()));
        html.append(meta("Locations", t.getLocations()));
        html.append(meta("Symbols", humanizeAll(t.getSymbols())));
        html.append(meta("Refers to", internalRefs(t.getInternalRefs())));
        return placed("table", t.getLocation(), html, t.getLanguage());
    }

    private static Placed calculation(Calculation c) {
        if (blank(c.getContent()) && c.getData().stream().allMatch(AnnotationHtml::blank)) return null;
        StringBuilder html = new StringBuilder();
        html.append(heading("Calculation", humanize(c.getType()), c.getMethod()));
        List<String> lines = new ArrayList<>();
        if (!blank(c.getContent())) {
            for (String line : c.getContent().split("\\R")) {
                if (!blank(line)) lines.add(esc(clean(line)));
            }
        }
        for (String d : c.getData()) {
            if (!blank(d)) lines.add(esc(clean(d)));
        }
        appendLines(html, lines);
        return placed("calculation", c.getLocation(), html, null);
    }

    private static Placed link(PhysicalLink link) {
        List<String> lines = new ArrayList<>();
        for (AnnotationLink al : link.getLinks()) {
            String line = join(" \u2192 ", al.source(), al.target());
            if (!blank(al.relationship())) line = join(" ", line, "(" + clean(al.relationship()) + ")");
            if (!line.isEmpty()) lines.add(esc(line));
        }
        if (lines.isEmpty()) return null;
        StringBuilder html = new StringBuilder(heading("Physical link", null, null));
        appendLines(html, lines);
        return placed("link", link.getLocation(), html, null);
    }

    // ---- Helpers ----

    private static void appendTexts(StringBuilder html, List<TextEl> texts) {
        for (TextEl t : texts) {
            if (blank(t.text())) continue;
            html.append("<p>");
            String detail = join(", ", languageName(t.language()), blank(t.hand()) ? null : humanize(t.hand()) + " hand");
            html.append(esc(clean(t.text())));
            if (!detail.isEmpty()) {
                html.append(" <small>(").append(esc(detail)).append(")</small>");
            }
            html.append("</p>");
        }
    }

    /** Appends already-escaped lines as one paragraph separated by line breaks. */
    private static void appendLines(StringBuilder html, List<String> escapedLines) {
        if (!escapedLines.isEmpty()) {
            html.append("<p>").append(String.join("<br>", escapedLines)).append("</p>");
        }
    }

    private static List<String> internalRefs(List<InternalReference> refs) {
        List<String> out = new ArrayList<>();
        for (InternalReference r : refs) {
            out.add(internalRef(r));
        }
        return out;
    }

    private static List<String> humanizeAll(List<String> names) {
        List<String> out = new ArrayList<>();
        for (String n : names) {
            out.add(humanize(n));
        }
        return out;
    }

    private static String humanizeLower(String name) {
        return blank(name) ? null : clean(name.replace('_', ' '));
    }
}
