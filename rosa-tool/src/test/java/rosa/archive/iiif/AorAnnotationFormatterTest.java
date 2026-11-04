package rosa.archive.iiif;

import org.junit.jupiter.api.Test;
import rosa.archive.aor.AorReferenceResolver;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Errata;
import rosa.archive.model.aor.InternalReference;
import rosa.archive.model.aor.Location;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.MarginaliaLanguage;
import rosa.archive.model.aor.Mark;
import rosa.archive.model.aor.Numeral;
import rosa.archive.model.aor.Position;
import rosa.archive.model.aor.ReferenceTarget;
import rosa.archive.model.aor.Symbol;
import rosa.archive.model.aor.Underline;
import rosa.archive.model.aor.XRef;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AorAnnotationFormatter}.
 */
class AorAnnotationFormatterTest {

    private static Position position(Location place, String text) {
        Position p = new Position();
        p.setPlace(place);
        p.getTexts().add(text);
        return p;
    }

    private static Marginalia marginalia(String lang, Position... positions) {
        Marginalia m = new Marginalia();
        m.setHand("Italian");
        MarginaliaLanguage ml = new MarginaliaLanguage();
        ml.setLang(lang);
        ml.getPositions().addAll(List.of(positions));
        m.getLanguages().add(ml);
        return m;
    }

    private static AnnotatedPage page() {
        AnnotatedPage page = new AnnotatedPage();
        page.setPagination("323");
        page.setSignature("X3r");
        page.setReader("Harvey");
        return page;
    }

    private static String classes(AnnotationItem item) {
        return String.join(" ", item.styleClasses());
    }

    @Test
    void pageHeaderComesFirst() {
        List<AnnotationItem> items = AorAnnotationFormatter.format(page());
        assertEquals(1, items.size());
        AnnotationItem header = items.get(0);
        assertEquals("rosa-anno aor-page", classes(header));
        assertEquals("<p><b>323</b> <small>Signature X3r</small></p><p><small><b>Reader:</b> Harvey</small></p>",
                header.html());
    }

    @Test
    void noHeaderForEmptyPage() {
        assertTrue(AorAnnotationFormatter.format(new AnnotatedPage()).isEmpty());
    }

    @Test
    void marginaliaIncludesTranslationPeopleAndCrossReferences() {
        Position pos = position(Location.RIGHT_MARGIN, " Euscopi fundamentu[m]\n   adama[n]tinum. ");
        pos.getPeople().add("Euscopus");
        pos.getXRefs().add(new XRef("Cicero", "De officiis", "honestum", "LA"));
        Marginalia m = marginalia("LA", pos);
        m.setTranslation("The adamantine foundation of Euscopus.");
        AnnotatedPage page = new AnnotatedPage();
        page.getMarginalia().add(m);

        AnnotationItem item = AorAnnotationFormatter.format(page).get(0);
        assertEquals("rosa-anno aor-marginalia aor-place-right_margin", classes(item));
        assertEquals("la", item.language());
        assertEquals("<p><b>Marginal note</b> <small>Latin, Italian hand</small></p>"
                + "<p>Euscopi fundamentu[m] adama[n]tinum.</p>"
                + "<p><i>The adamantine foundation of Euscopus.</i></p>"
                + "<p><small><b>People:</b> Euscopus</small></p>"
                + "<p><small><b>Cross-references:</b> Cicero, De officiis: \u201chonestum\u201d</small></p>",
                item.html());
    }

    @Test
    void namesLinkToTheAdvancedSearchWhenLinksAreConfigured() {
        Position pos = position(Location.INTEXT, "Excidium Troia[e], secundum Plutarchum");
        pos.getPeople().add("Plutarch");
        pos.getPeople().add("Dionysius of Halicarnassus");
        pos.getBooks().add("Chronicum");
        pos.getLocations().add("Troy");
        AnnotatedPage page = new AnnotatedPage();
        page.getMarginalia().add(marginalia("LA", pos));

        AorLinks links = new AorLinks(new AorSearchLinks("https://example.org/aor/", "aor"), null);
        AnnotationItem item = AorAnnotationFormatter.format(page, links).get(0);
        assertEquals("<p><b>Marginal note</b> <small>Latin, Italian hand</small></p>"
                + "<p>Excidium Troia[e], secundum Plutarchum</p>"
                + "<p><small><b>People:</b> "
                + "<a href=\"https://example.org/aor/#aor/search?q=people:'%22Plutarch%22'&amp;type=advanced\">Plutarch</a>, "
                + "<a href=\"https://example.org/aor/#aor/search?q=people:'%22Dionysius+of+Halicarnassus%22'&amp;type=advanced\">Dionysius of Halicarnassus</a>"
                + "</small></p>"
                + "<p><small><b>Books:</b> "
                + "<a href=\"https://example.org/aor/#aor/search?q=books:'%22Chronicum%22'&amp;type=advanced\">Chronicum</a></small></p>"
                + "<p><small><b>Locations:</b> "
                + "<a href=\"https://example.org/aor/#aor/search?q=locations:'%22Troy%22'&amp;type=advanced\">Troy</a></small></p>",
                item.html());
        assertNull(AnnotationHtml.findViolation(item.html()));

        // Without links the names are plain text, as before
        assertTrue(AorAnnotationFormatter.format(page).get(0).html()
                .contains("<p><small><b>People:</b> Plutarch, Dionysius of Halicarnassus</small></p>"));
    }

    @Test
    void linkedNamesAreEscapedAndCompliant() {
        Position pos = position(Location.INTEXT, "x");
        pos.getPeople().add("a < b & \"c\" 'd' \\");
        AnnotatedPage page = new AnnotatedPage();
        page.getMarginalia().add(marginalia("LA", pos));

        AnnotationItem item = AorAnnotationFormatter.format(page,
                new AorLinks(new AorSearchLinks("https://example.org/aor/", "aor"), null)).get(0);
        assertNull(AnnotationHtml.findViolation(item.html()), item.html());
        assertTrue(item.html().contains(
                "<a href=\"https://example.org/aor/#aor/search?q=people:'%22a+%3C+b+%26+c+%5C'd%5C'+%5C%5C%22'&amp;type=advanced\">"
                + "a &lt; b &amp; &quot;c&quot; 'd' \\</a>"), item.html());
    }

    @Test
    void crossReferencesAndInternalReferencesLinkWhenConfigured() {
        Position pos = position(Location.INTEXT, "Vide fol[io] 35.");
        pos.getXRefs().add(new XRef("Plutarch", "Moralia", "Demadi, sus Minerva[e]", "LA"));
        InternalReference ref = new InternalReference();
        ref.setText("infra");
        ref.addTargets(new ReferenceTarget("BL615d7:615_d_7_f035r:m0001", "fol[io] 35", "[the] ", " leafe"),
                new ReferenceTarget("Nope:1", "60"));
        pos.getInternalRefs().add(ref);
        AnnotatedPage page = new AnnotatedPage();
        page.getMarginalia().add(marginalia("LA", pos));

        AorReferenceResolver.Target target = new AorReferenceResolver.Target("BL615d7", "BL615d7.035r.tif", 40);
        AorLinks links = new AorLinks(new AorSearchLinks("https://example.org/aor/", "aor"),
                r -> r.startsWith("BL615d7:") ? Optional.of(target) : Optional.empty());
        String html = AorAnnotationFormatter.format(page, links).get(0).html();
        assertTrue(html.contains("<p><small><b>Cross-references:</b> "
                + "<a href=\"https://example.org/aor/#aor/search?q=cross_reference:'%22Plutarch%22'&amp;type=advanced\">Plutarch</a>, "
                + "<a href=\"https://example.org/aor/#aor/search?q=cross_reference:'%22Moralia%22'&amp;type=advanced\">Moralia</a>: "
                + "\u201cDemadi, sus Minerva[e]\u201d</small></p>"), html);
        assertTrue(html.contains("<p><small><b>Refers to:</b> infra \u2192 "
                + "[the] <a href=\"https://example.org/aor/#aor/BL615d7/035r/image\">\u201cfol[io] 35\u201d</a> leafe; "
                + "\u201c60\u201d</small></p>"), html);
        assertNull(AnnotationHtml.findViolation(html), html);

        // Without links: the same lines as plain text, the target ref itself never shown
        String plain = AorAnnotationFormatter.format(page).get(0).html();
        assertTrue(plain.contains("<p><small><b>Cross-references:</b> Plutarch, Moralia: \u201cDemadi, sus Minerva[e]\u201d</small></p>"), plain);
        assertTrue(plain.contains("<p><small><b>Refers to:</b> infra \u2192 [the] \u201cfol[io] 35\u201d leafe; \u201c60\u201d</small></p>"), plain);
        assertFalse(plain.contains("<a "));
    }

    @Test
    void targetWithoutTextIsLabelledByItsPage() {
        Position pos = position(Location.INTEXT, "x");
        InternalReference ref = new InternalReference();
        ref.addTargets(new ReferenceTarget("RCPD1266:84", ""));
        pos.getInternalRefs().add(ref);
        AnnotatedPage page = new AnnotatedPage();
        page.getMarginalia().add(marginalia("LA", pos));

        AorLinks links = new AorLinks(new AorSearchLinks("https://example.org/aor/", "aor"),
                _ -> Optional.of(new AorReferenceResolver.Target("RCPD1266", "RCPD1266.037v.tif", 83)));
        assertTrue(AorAnnotationFormatter.format(page, links).get(0).html().contains(
                "<p><small><b>Refers to:</b> <a href=\"https://example.org/aor/#aor/RCPD1266/037v/image\">037v</a></small></p>"));
        // Without a resolver the empty target vanishes, as before
        assertFalse(AorAnnotationFormatter.format(page).get(0).html().contains("Refers to"));
    }

    @Test
    void multiPlaceMarginaliaUsesFirstPlaceAndLabelsTheRest() {
        Marginalia m = marginalia("LA",
                position(Location.INTEXT, "Ante omnes auctores,"),
                position(Location.RIGHT_MARGIN, "Jureconsult[i]s Vigelij:"),
                position(Location.INTEXT, "apophthegmaticus Zuingeri."));
        AnnotatedPage page = new AnnotatedPage();
        page.getMarginalia().add(m);

        AnnotationItem item = AorAnnotationFormatter.format(page).get(0);
        assertTrue(classes(item).endsWith("aor-place-intext"));
        assertTrue(item.html().contains("<p>Ante omnes auctores,</p>"
                + "<p><small>[Right margin]</small> Jureconsult[i]s Vigelij:</p>"
                + "<p>apophthegmaticus Zuingeri.</p>"), item.html());
    }

    @Test
    void underlineDefaultsToInText() {
        AnnotatedPage page = new AnnotatedPage();
        page.getUnderlines().add(new Underline(null, "ego solus, qui", "pen", "straight", "LA"));
        AnnotationItem item = AorAnnotationFormatter.format(page).get(0);
        assertEquals("rosa-anno aor-underline aor-place-intext", classes(item));
        assertEquals("<p><b>Underline</b> <small>straight, pen</small></p><p>\u201cego solus, qui\u201d</p>", item.html());
    }

    @Test
    void marksSymbolsNumeralsAndErrataAreReadable() {
        AnnotatedPage page = new AnnotatedPage();
        page.getMarks().add(new Mark(null, "artium usum", "plus_sign", "pen", "LA", Location.INTEXT));
        page.getMarks().add(new Mark(null, null, "dot", "pen", null, Location.INTEXT));
        page.getSymbols().add(new Symbol(null, null, "Bisected_circle", null, Location.LEFT_MARGIN));
        page.getNumerals().add(new Numeral(null, null, "1.", null, Location.INTEXT));
        page.getErrata().add(new Errata(null, "LA", "Saturni", "Saturno"));

        List<String> html = AorAnnotationFormatter.format(page).stream().map(AnnotationItem::html).toList();
        assertTrue(html.contains("<p><b>Mark</b> Plus sign <small>pen</small></p><p>\u201cartium usum\u201d</p>"), html.toString());
        assertTrue(html.contains("<p><b>Mark</b> Dot <small>pen</small></p>"));
        assertTrue(html.contains("<p><b>Symbol</b> Bisected circle</p>"));
        assertTrue(html.contains("<p><b>Numeral</b> 1.</p>"));
        assertTrue(html.contains("<p><b>Erratum</b></p><p>\u201cSaturni\u201d \u2192 \u201cSaturno\u201d</p>"));
    }

    @Test
    void itemsAreOrderedByPlace() {
        AnnotatedPage page = page();
        page.getMarks().add(new Mark(null, null, "dash", "pen", null, Location.TAIL));
        page.getMarks().add(new Mark(null, null, "dot", "pen", null, null));
        page.getMarks().add(new Mark(null, null, "dash", "pen", null, Location.RIGHT_MARGIN));
        page.getSymbols().add(new Symbol(null, null, "Sun", null, Location.HEAD));
        page.getMarginalia().add(marginalia("LA", position(Location.LEFT_MARGIN, "nota")));
        page.getUnderlines().add(new Underline(null, "x", "pen", "straight", "LA"));

        List<String> order = AorAnnotationFormatter.format(page).stream()
                .map(i -> i.styleClasses().size() > 2 ? i.styleClasses().get(2) : i.styleClasses().get(1))
                .toList();
        assertEquals(List.of("aor-page", "aor-place-head", "aor-place-left_margin", "aor-place-intext",
                "aor-place-right_margin", "aor-place-tail", "aor-mark"), order);
    }

    @Test
    void outputIsCompliantHtml() {
        AnnotatedPage page = page();
        Marginalia m = marginalia("LA", position(Location.HEAD, "a < b & \"c\""));
        m.setTranslation("<script>");
        page.getMarginalia().add(m);
        page.getUnderlines().add(new Underline(null, "<b>", "pen", "straight", "LA"));
        for (AnnotationItem item : AorAnnotationFormatter.format(page)) {
            assertNull(AnnotationHtml.findViolation(item.html()), item.html());
            assertFalse(item.html().contains("<script>"));
        }
    }
}
