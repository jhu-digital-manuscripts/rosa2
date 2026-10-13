package rosa.archive.iiif;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AnnotationHtml}.
 */
class AnnotationHtmlTest {

    @Test
    void escapesAndCleansText() {
        assertEquals("a &amp; b &lt;i&gt; &quot;x&quot;", AnnotationHtml.esc("a & b <i> \"x\""));
        assertEquals("one two", AnnotationHtml.clean("  one \n\t two "));
        assertEquals("", AnnotationHtml.esc(null));
    }

    @Test
    void humanizesArchiveNames() {
        assertEquals("Quotation mark", AnnotationHtml.humanize("quotation_mark"));
        assertEquals("Bisected circle", AnnotationHtml.humanize("Bisected_circle"));
        assertEquals("SS", AnnotationHtml.humanize("SS"));
        assertEquals("", AnnotationHtml.humanize(null));
    }

    @Test
    void buildsConventionalParagraphs() {
        assertEquals("<p><b>Mark</b> Dot <small>pen</small></p>", AnnotationHtml.heading("Mark", "Dot", "pen"));
        assertEquals("<p><b>Erratum</b></p>", AnnotationHtml.heading("Erratum", null, ""));
        assertEquals("<p><i>The threefold anchor.</i></p>", AnnotationHtml.translation(" The threefold\n anchor. "));
        assertEquals("<p><small><b>People:</b> Bodin, Vigel</small></p>",
                AnnotationHtml.meta("People", List.of("Bodin", " ", "Vigel", "Bodin")));
        assertEquals("", AnnotationHtml.meta("People", List.of()));
    }

    @Test
    void normalizesLanguages() {
        assertEquals("la", AnnotationHtml.normalizeLanguage("LA"));
        assertNull(AnnotationHtml.normalizeLanguage(""));
        assertNull(AnnotationHtml.normalizeLanguage("none"));
        assertEquals("Latin", AnnotationHtml.languageName("LA"));
        assertEquals("XX", AnnotationHtml.languageName("XX"));
    }

    @Test
    void acceptsCompliantHtml() {
        assertNull(AnnotationHtml.findViolation("<p><b>x</b> <small>y</small><br><sub>5</sub><sup>10</sup></p>"));
        assertNull(AnnotationHtml.findViolation("<p><a href=\"http://x\">l</a><img src=\"i\" alt=\"a\"/></p>"));
        assertNull(AnnotationHtml.findViolation("<span>x</span>"));
    }

    @Test
    void rejectsNonCompliantHtml() {
        assertNotNull(AnnotationHtml.findViolation("plain text"));
        assertNotNull(AnnotationHtml.findViolation("<div>x</div>"));
        assertNotNull(AnnotationHtml.findViolation("<p class=\"x\">x</p>"));
        assertNotNull(AnnotationHtml.findViolation("<p><b>x</p></b>"));
        assertNotNull(AnnotationHtml.findViolation("<p><b>x</b>"));
        assertNotNull(AnnotationHtml.findViolation("<a target=\"_blank\" href=\"x\">x</a>"));
    }
}
