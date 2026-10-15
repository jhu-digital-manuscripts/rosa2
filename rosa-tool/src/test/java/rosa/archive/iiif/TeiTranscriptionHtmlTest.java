package rosa.archive.iiif;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TeiTranscriptionHtml}.
 */
class TeiTranscriptionHtmlTest {

    private static String tei(String body) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><TEI xmlns=\"http://www.tei-c.org/ns/1.0\">"
                + "<teiHeader><fileDesc><titleStmt><title>ignored header</title></titleStmt></fileDesc></teiHeader>"
                + "<text><body><div type=\"ms\">" + body + "</div></body></text></TEI>";
    }

    private static String lecoyLines(int from, int to) {
        StringBuilder sb = new StringBuilder("<lg type=\"couplet\">");
        for (int i = from; i <= to; i++) {
            sb.append("<l>line ").append(i).append("<milestone n=\"").append(i)
                    .append("\" ed=\"lecoy\" unit=\"line\"/>\n</l>\n");
        }
        return sb.append("</lg>").toString();
    }

    @Test
    void splitsColumnsAndContinuesRepeatedPageBreaks() {
        // Douce332 repeats <pb n="001r"/> before each column
        String xml = tei("<pb n=\"001r\"/><cb n=\"a\"/>"
                + "<figure type=\"miniature\"><head>Lover in bed</head>"
                + "<note type=\"character\">Lover</note><note type=\"character\">Danger</note></figure>"
                + lecoyLines(1, 2)
                + "<pb n=\"001r\"/><cb n=\"b\"/>" + lecoyLines(3, 5)
                + "<pb n=\"001v\"/><cb n=\"a\"/>" + lecoyLines(6, 6));

        Map<String, List<TeiTranscriptionHtml.Column>> pages = TeiTranscriptionHtml.convert(xml);
        assertEquals(List.of("001r", "001v"), List.copyOf(pages.keySet()));

        List<TeiTranscriptionHtml.Column> cols = pages.get("001r");
        assertEquals(2, cols.size());
        assertEquals("a", cols.get(0).name());
        assertEquals("<p><b>Column a</b></p>"
                + "<p><b>Miniature:</b> <i>Lover in bed</i><br><small><b>Characters:</b> Lover, Danger</small></p>"
                + "<p><span>line 1</span><br><span>line 2</span></p>", cols.get(0).html());
        assertEquals("<p><b>Column b</b></p>"
                + "<p><span>line 3</span><br><span>line 4</span><br><span>line 5 <sup>5</sup></span></p>",
                cols.get(1).html());
        assertFalse(cols.get(0).html().contains("ignored header"));
    }

    @Test
    void showsEveryFifthLogicalLineNumber() {
        StringBuilder lines = new StringBuilder("<pb n=\"1r\"/><lg>");
        for (int i = 1; i <= 10; i++) {
            lines.append("<l n=\"").append(i).append("\">v").append(i).append("</l>");
        }
        lines.append("</lg>");
        String html = TeiTranscriptionHtml.convert(tei(lines.toString())).get("1r").get(0).html();
        assertTrue(html.contains("<span><sub>5</sub> v5</span>"), html);
        assertTrue(html.contains("<span><sub>10</sub> v10</span>"), html);
        assertFalse(html.contains("<sub>4</sub>"), html);
        assertTrue(html.startsWith("<p><span>v1</span>"), "No column heading when the page has no cb: " + html);
    }

    @Test
    void readsLecoyMilestonesThatFollowTheLine() {
        // Morgan948 puts the milestone after </l>, zero-padded, alongside l/@n
        String xml = tei("<pb n=\"1r\"/><lg type=\"couplet\">"
                + "<l n=\"4634\">Trop sont</l>\n<milestone n=\"4585\" ed=\"lecoy\" unit=\"line\"/>\n"
                + "<l n=\"4635\">Cueurs qui</l>\n<milestone n=\"4586\" ed=\"lecoy\" unit=\"line\"/>\n"
                + "</lg><lg type=\"couplet\">"
                + "<l n=\"4\">Qui ne sont</l>\n<milestone n=\"0005\" ed=\"lecoy\" unit=\"line\"/>\n"
                + "</lg>");
        String html = TeiTranscriptionHtml.convert(xml).get("1r").get(0).html();
        assertEquals("<p><span>Trop sont <sup>4585</sup></span><br><span><sub>4635</sub> Cueurs qui</span></p>"
                + "<p><span>Qui ne sont <sup>5</sup></span></p>", html);
    }

    @Test
    void doesNotTakeAMilestoneAcrossOtherContent() {
        String xml = tei("<pb n=\"1r\"/><lg><l>one</l><l>two</l>"
                + "<milestone n=\"5\" ed=\"lecoy\" unit=\"line\"/></lg>");
        String html = TeiTranscriptionHtml.convert(xml).get("1r").get(0).html();
        assertEquals("<p><span>one</span><br><span>two <sup>5</sup></span></p>", html);
    }

    @Test
    void formatsInlineMarkupAndNotes() {
        String xml = tei("<pb n=\"2r\"/><hi rend=\"rubric\">Cy commence</hi>"
                + "<lg><l><hi rend=\"init\">M</hi>ainte<expan>s</expan> gens<note type=\"critical\">\"p. tex\" (Lecoy)</note></l>"
                + "<l>descriu<add>r</add>e <del>x</del><note type=\"scribalPunc\">.</note>"
                + "<note type=\"correction\">Later hand</note></l></lg>"
                + "<gap><desc>647-648</desc></gap><fw type=\"catch\">Ja soit</fw>");
        String html = TeiTranscriptionHtml.convert(xml).get("2r").get(0).html();
        assertEquals("<p><b><i>Cy commence</i></b></p>"
                + "<p><span><b>M</b>ainte<i>s</i> gens<small>*&nbsp;&quot;p. tex&quot; (Lecoy)</small></span>"
                + "<br><span>descriu\u2e0cr\u2e0de \u27e6x\u27e7.<small>*&nbsp;Correction: Later hand</small></span></p>"
                + "<p><small>[Gap: 647-648]</small></p>"
                + "<p><small><b>Catchword:</b> Ja soit</small></p>", html);
        assertNull(AnnotationHtml.findViolation(html), html);
    }

    @Test
    void findsPagesByImageIdVariants() {
        Map<String, List<TeiTranscriptionHtml.Column>> pages =
                TeiTranscriptionHtml.convert(tei("<pb n=\"1r\"/><lg><l>a</l></lg><pb n=\"010v\"/><lg><l>b</l></lg>"));
        assertEquals(1, TeiTranscriptionHtml.findPage(pages, "Book.001r.tif").size());
        assertEquals(1, TeiTranscriptionHtml.findPage(pages, "Book.010v.tif").size());
        assertTrue(TeiTranscriptionHtml.findPage(pages, "Book.002r.tif").isEmpty());
        assertTrue(TeiTranscriptionHtml.findPage(pages, null).isEmpty());
    }

    @Test
    void lineNumberRule() {
        assertTrue(TeiTranscriptionHtml.showNumber("15"));
        assertTrue(TeiTranscriptionHtml.showNumber("20a"));
        assertFalse(TeiTranscriptionHtml.showNumber("16"));
        assertFalse(TeiTranscriptionHtml.showNumber("a"));
        assertFalse(TeiTranscriptionHtml.showNumber(null));
    }

    @Test
    void rejectsMalformedXml() {
        assertThrows(IllegalArgumentException.class, () -> TeiTranscriptionHtml.convert("<TEI><pb"));
    }
}
