package rosa.archive.opensearch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TranscriptionXmlExtractor}.
 */
class TranscriptionXmlExtractorTest {

    private static TranscriptionXmlExtractor.Result extract(String body) {
        return TranscriptionXmlExtractor.extract("test", "<div type=\"ms\">" + body + "</div>");
    }

    @Test
    @DisplayName("Extracts poetry from <l> elements")
    void extractsPoetry() {
        TranscriptionXmlExtractor.Result result = extract("<lg type=\"couplet\">" +
                "<l n=\"1\">Maintes gens dient que en songes</l>" +
                "<l n=\"2\">N'a se fables non et mensonges</l>" +
                "</lg>");

        assertEquals("Maintes gens dient que en songes N'a se fables non et mensonges", result.poetry());
    }

    @Test
    @DisplayName("Skips line numbers from <l n='...'> and Lecoy numbers from <milestone n='...'>")
    void skipsLineAndLecoyNumbers() {
        TranscriptionXmlExtractor.Result result = extract(
                "<milestone ed=\"lecoy\" unit=\"line\" n=\"7305\"/>" +
                "<lg type=\"couplet\">" +
                "<l n=\"101\">Some text<milestone n=\"102\" ed=\"lecoy\" unit=\"line\"/></l>" +
                "<l n=\"102\">More text</l>" +
                "</lg>");

        assertEquals("Some text More text", result.poetry());
        assertEquals(List.of(), result.rubrics());
        assertEquals(List.of(), result.catchphrases());
        assertEquals(List.of(), result.illustrations());
        assertEquals(List.of(), result.notes());
    }

    @Test
    @DisplayName("A line continues after an initial: <hi rend='init'>M</hi>aintes is 'Maintes'")
    void initialDoesNotEndTheLine() {
        TranscriptionXmlExtractor.Result result = extract("<lg type=\"couplet\">" +
                "<l n=\"1\"><hi rend=\"init\">M</hi>ainte<expan>s</expan> ge<expan>n</expan>s dient qu'en songes</l>" +
                "<l n=\"2\">N'a se fables non et mensonges</l>" +
                "</lg>");

        assertEquals("Maintes gens dient qu'en songes N'a se fables non et mensonges", result.poetry());
    }

    @Test
    @DisplayName("A line continues after an editorial note, and the note is English, not verse")
    void noteInsideLineIsSeparated() {
        TranscriptionXmlExtractor.Result result = extract("<lg type=\"couplet\">" +
                "<l n=\"17\">Des biens aux gens et des ennuys</l>" +
                "<l n=\"18\">Car<note type=\"critical\"> \"que\" (Lecoy)</note> li plusieurs songent de nuys</l>" +
                "</lg>");

        assertEquals("Des biens aux gens et des ennuys Car li plusieurs songent de nuys", result.poetry());
        assertEquals(List.of("\"que\" (Lecoy)"), result.notes());
    }

    @Test
    @DisplayName("Each note is its own entry")
    void notesAreSeparateEntries() {
        TranscriptionXmlExtractor.Result result = extract("<lg type=\"couplet\">" +
                "<l>Qui ne les tint pas a lobes<note type=\"critical\">\"q. ne tint pas songes a lobes\" (Lecoy).</note></l>" +
                "<l>Ainçoys descript<note type=\"critical\">\"escrit\" (Lecoy)</note> l'advision</l>" +
                "</lg>" +
                "<l><note type=\"order\">Reordered; to be read after 'a.' above</note>Si com li contes recensoit</l>");

        assertEquals("Qui ne les tint pas a lobes Ainçoys descript l'advision Si com li contes recensoit",
                result.poetry());
        assertEquals(List.of(
                "\"q. ne tint pas songes a lobes\" (Lecoy).",
                "\"escrit\" (Lecoy)",
                "Reordered; to be read after 'a.' above"), result.notes());
    }

    @Test
    @DisplayName("Extracts rubrics from <hi rend='rubric'>, also inside a line, as separate entries")
    void extractsRubrics() {
        TranscriptionXmlExtractor.Result result = extract(
                "<hi rend=\"rubric\">Ci commence le rommant de la rose</hi>" +
                "<l><hi rend=\"rubric\">Ci parle l'Amant</hi></l>" +
                "<lg type=\"couplet\"><l>Si com l'istoire le reçoit</l></lg>");

        assertEquals(List.of("Ci commence le rommant de la rose", "Ci parle l'Amant"), result.rubrics());
        assertEquals("Si com l'istoire le reçoit", result.poetry());
    }

    @Test
    @DisplayName("A rubric continues after an initial and after a note inside it")
    void rubricWithInitialAndNote() {
        TranscriptionXmlExtractor.Result result = extract("<l><hi rend=\"rubric\"><hi rend=\"init\">C</hi>y est le Rommant de la rose " +
                "ou tout l'art d'amo<expan>ur</expan>s est enclose" +
                "<note type=\"critical\">This pair of lines does not appear in Lecoy.</note></hi></l>");

        assertEquals(List.of("Cy est le Rommant de la rose ou tout l'art d'amours est enclose"), result.rubrics());
        assertEquals(List.of("This pair of lines does not appear in Lecoy."), result.notes());
        assertEquals("", result.poetry());
    }

    @Test
    @DisplayName("Extracts catchwords from <fw>")
    void extractsCatchphrases() {
        TranscriptionXmlExtractor.Result result = extract(
                "<fw type=\"catch\">Que tuit</fw><fw type=\"catch\">Mar puissent il estre arivez</fw>");

        assertEquals(List.of("Que tuit", "Mar puissent il estre arivez"), result.catchphrases());
    }

    @Test
    @DisplayName("A figure is one entry made of its head and character notes")
    void extractsIllustrations() {
        TranscriptionXmlExtractor.Result result = extract(
                "<figure type=\"miniature\"><head>Lover sleeping</head>" +
                "<note type=\"character\">Lover</note><note type=\"character\">Danger</note></figure>" +
                "<figure type=\"miniature\"><head>Lover dressing</head><note type=\"character\">Lover</note></figure>" +
                "<lg type=\"couplet\"><l>Maintes gens dient que en songes</l></lg>");

        assertEquals(List.of("Lover sleeping Lover Danger", "Lover dressing Lover"), result.illustrations());
        assertEquals(List.of(), result.notes());
        assertEquals("Maintes gens dient que en songes", result.poetry());
    }

    @Test
    @DisplayName("A character note outside a figure is still an illustration entry")
    void characterNoteOutsideFigure() {
        TranscriptionXmlExtractor.Result result = extract("<note type=\"character\">The Lover</note>");

        assertEquals(List.of("The Lover"), result.illustrations());
    }

    @Test
    @DisplayName("Extracts general notes from <note type='other'>")
    void extractsNotes() {
        TranscriptionXmlExtractor.Result result = extract("<note type=\"editorial\">Added by scribe</note>");

        assertEquals(List.of("Added by scribe"), result.notes());
    }

    @Test
    @DisplayName("Ignores scribal punctuation notes but keeps the rest of the line")
    void ignoresScribalPunctuation() {
        TranscriptionXmlExtractor.Result result = extract("<lg type=\"couplet\">" +
                "<l><hi rend=\"init\">M</hi>aintes genz audent que en songes<note type=\"scribalPunc\">.</note></l>" +
                "<l>N'ait se fables non et mensonges;<note type=\"scribalPun\">.'</note></l>" +
                "</lg>");

        assertEquals("Maintes genz audent que en songes N'ait se fables non et mensonges;", result.poetry());
        assertEquals(List.of(), result.notes());
    }

    @Test
    @DisplayName("Skips gaps and their <desc> (Lecoy numbers of missing lines)")
    void skipsGaps() {
        TranscriptionXmlExtractor.Result result = extract("<lg type=\"couplet\">" +
                "<l>Si en puis bien traire a garant<gap><desc>655-656</desc></gap></l>" +
                "<l>Qui ne tint pas songes a lobes</l>" +
                "</lg>");

        assertEquals("Si en puis bien traire a garant Qui ne tint pas songes a lobes", result.poetry());
    }

    @Test
    @DisplayName("Keeps additions, deletions and expansions as part of the line")
    void keepsAdditionsAndDeletions() {
        TranscriptionXmlExtractor.Result result = extract(
                "<l>Qui <del>onl</del><add>ne</add> tint pas so<expan>n</expan>ges a lobes</l>");

        assertEquals("Qui onlne tint pas songes a lobes", result.poetry());
    }

    @Test
    @DisplayName("Collapses whitespace inside every entry")
    void normalizesWhitespace() {
        TranscriptionXmlExtractor.Result result = extract(
                "<figure type=\"miniature\">\n      <head>Miniature spanning both columns.  Left: Guillaume de Lorris. </head>\n    </figure>\n" +
                "<l>\n    <hi rend=\"rubric\">Cy commence le Rommant de la rose</hi>\n  </l>");

        assertEquals(List.of("Miniature spanning both columns. Left: Guillaume de Lorris."), result.illustrations());
        assertEquals(List.of("Cy commence le Rommant de la rose"), result.rubrics());
    }

    @Test
    @DisplayName("oldFrench() and english() group the categories for the language sub-fields")
    void languageGroups() {
        TranscriptionXmlExtractor.Result result = extract(
                "<figure type=\"miniature\"><head>Lover sleeping</head></figure>" +
                "<hi rend=\"rubric\">Ci commence</hi>" +
                "<lg type=\"couplet\"><l>Maintes gens<note type=\"critical\">note</note></l></lg>" +
                "<fw type=\"catch\">Que tuit</fw>");

        assertEquals(List.of("Maintes gens", "Ci commence", "Que tuit"), result.oldFrench());
        assertEquals(List.of("Lover sleeping", "note"), result.english());
    }

    @Test
    @DisplayName("Returns empty result for null, blank and unparsable input")
    void handlesBadInput() {
        for (String xml : new String[]{null, "   ", "<div><l>unclosed</div>"}) {
            TranscriptionXmlExtractor.Result result = TranscriptionXmlExtractor.extract("test", xml);
            assertEquals("", result.poetry());
            assertEquals(List.of(), result.oldFrench());
            assertEquals(List.of(), result.english());
        }
    }
}
