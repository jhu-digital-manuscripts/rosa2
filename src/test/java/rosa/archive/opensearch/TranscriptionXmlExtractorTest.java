package rosa.archive.opensearch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TranscriptionXmlExtractor}.
 */
class TranscriptionXmlExtractorTest {

    @Test
    @DisplayName("Extracts poetry from <l> elements")
    void extractsPoetry() {
        String xml = "<div type=\"ms\"><lg type=\"couplet\">" +
                "<l n=\"1\">Maintes gens dient que en songes</l>" +
                "<l n=\"2\">N'a se fables non et mensonges</l>" +
                "</lg></div>";

        TranscriptionXmlExtractor.Result result = TranscriptionXmlExtractor.extract(xml);

        assertTrue(result.poetry().contains("Maintes gens dient que en songes"));
        assertTrue(result.poetry().contains("N'a se fables non et mensonges"));
    }

    @Test
    @DisplayName("Extracts line numbers from <l n='...'>")
    void extractsLineNumbers() {
        String xml = "<div type=\"ms\"><lg type=\"couplet\">" +
                "<l n=\"101\">Some text</l>" +
                "<l n=\"102\">More text</l>" +
                "</lg></div>";

        TranscriptionXmlExtractor.Result result = TranscriptionXmlExtractor.extract(xml);

        assertTrue(result.line().contains("101"));
        assertTrue(result.line().contains("102"));
    }

    @Test
    @DisplayName("Extracts Lecoy numbers from <milestone n='...'>")
    void extractsLecoyNumbers() {
        String xml = "<div type=\"ms\"><milestone n=\"L101\"/>" +
                "<lg type=\"couplet\"><l n=\"1\">text</l></lg></div>";

        TranscriptionXmlExtractor.Result result = TranscriptionXmlExtractor.extract(xml);

        assertTrue(result.lecoy().contains("L101"));
    }

    @Test
    @DisplayName("Extracts rubrics from <hi rend='rubric'>")
    void extractsRubrics() {
        String xml = "<div type=\"ms\"><hi rend=\"rubric\">Ci commence le rommant de la rose</hi></div>";

        TranscriptionXmlExtractor.Result result = TranscriptionXmlExtractor.extract(xml);

        assertTrue(result.rubric().contains("Ci commence le rommant de la rose"));
    }

    @Test
    @DisplayName("Extracts catchphrases from <fw>")
    void extractsCatchphrases() {
        String xml = "<div type=\"ms\"><fw>catch phrase text</fw></div>";

        TranscriptionXmlExtractor.Result result = TranscriptionXmlExtractor.extract(xml);

        assertTrue(result.catchphrase().contains("catch phrase text"));
    }

    @Test
    @DisplayName("Extracts illustration notes from <figure> and <note type='character'>")
    void extractsIllustrations() {
        String xml = "<div type=\"ms\">" +
                "<figure>A garden scene</figure>" +
                "<note type=\"character\">The Lover</note>" +
                "</div>";

        TranscriptionXmlExtractor.Result result = TranscriptionXmlExtractor.extract(xml);

        assertTrue(result.illustration().contains("A garden scene"));
        assertTrue(result.illustration().contains("The Lover"));
    }

    @Test
    @DisplayName("Extracts general notes from <note type='other'>")
    void extractsNotes() {
        String xml = "<div type=\"ms\"><note type=\"editorial\">Added by scribe</note></div>";

        TranscriptionXmlExtractor.Result result = TranscriptionXmlExtractor.extract(xml);

        assertTrue(result.note().contains("Added by scribe"));
    }

    @Test
    @DisplayName("Returns empty result for null input")
    void handlesNullInput() {
        TranscriptionXmlExtractor.Result result = TranscriptionXmlExtractor.extract(null);

        assertEquals("", result.poetry());
        assertEquals("", result.rubric());
        assertEquals("", result.catchphrase());
        assertEquals("", result.illustration());
        assertEquals("", result.lecoy());
        assertEquals("", result.line());
        assertEquals("", result.note());
    }

    @Test
    @DisplayName("Returns empty result for blank input")
    void handlesBlankInput() {
        TranscriptionXmlExtractor.Result result = TranscriptionXmlExtractor.extract("   ");

        assertEquals("", result.poetry());
    }

    @Test
    @DisplayName("Ignores scribalPun notes")
    void ignoresScribalPun() {
        String xml = "<div type=\"ms\"><note type=\"scribalPun\">pun text</note></div>";

        TranscriptionXmlExtractor.Result result = TranscriptionXmlExtractor.extract(xml);

        assertEquals("", result.note());
        assertEquals("", result.illustration());
    }
}
