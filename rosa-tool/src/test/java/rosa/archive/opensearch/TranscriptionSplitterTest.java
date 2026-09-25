package rosa.archive.opensearch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TranscriptionSplitter}.
 */
class TranscriptionSplitterTest {

    private static String tei(String body) {
        return "<TEI><teiHeader><fileDesc/></teiHeader><text><body>" + body + "</body></text></TEI>";
    }

    @Test
    @DisplayName("Splits pages on <pb> and normalizes page names")
    void splitsPages() {
        Map<String, String> pages = TranscriptionSplitter.split(tei(
                "<pb n=\"1r\"/><lg type=\"couplet\"><l n=\"1\">One</l></lg>"
                        + "<pb n=\"12v\"/><lg type=\"couplet\"><l n=\"2\">Two</l></lg>"));

        assertEquals(2, pages.size());
        assertTrue(pages.containsKey("001r"));
        assertTrue(pages.containsKey("012v"));
        assertTrue(pages.get("001r").contains("One"));
        assertTrue(pages.get("012v").contains("Two"));
    }

    @Test
    @DisplayName("Re-escapes markup characters in text so fragments stay well-formed")
    void escapesTextContent() {
        // Source text contains an escaped ampersand immediately followed by an element.
        Map<String, String> pages = TranscriptionSplitter.split(tei(
                "<pb n=\"1r\"/><lg type=\"couplet\">"
                        + "<l n=\"21310\">En lieu de messe chansonnecte&amp;<expan>s</expan></l>"
                        + "<head>Abstinence Contrainte&amp;two clerics</head>"
                        + "</lg>"));

        String fragment = pages.get("001r");
        assertNotNull(fragment);
        assertFalse(fragment.contains("&<"), "Raw ampersand must not be emitted: " + fragment);
        assertTrue(fragment.contains("chansonnecte&amp;"), fragment);
        assertTrue(fragment.contains("Contrainte&amp;two"), fragment);

        // The fragment must parse cleanly, which is what the ingest generator does next.
        TranscriptionXmlExtractor.Result result = TranscriptionXmlExtractor.extract("test", fragment);
        assertTrue(result.poetry().contains("chansonnecte&s"), result.poetry());
    }

    @Test
    @DisplayName("Re-escapes markup characters in attribute values")
    void escapesAttributeValues() {
        Map<String, String> pages = TranscriptionSplitter.split(tei(
                "<pb n=\"1r\"/><lg type=\"couplet\">"
                        + "<note type=\"editorial\" resp=\"A &amp; B &lt;ed&gt;\">Note text</note>"
                        + "</lg>"));

        String fragment = pages.get("001r");
        assertNotNull(fragment);
        assertTrue(fragment.contains("resp=\"A &amp; B &lt;ed&gt;\""), fragment);

        TranscriptionXmlExtractor.Result result = TranscriptionXmlExtractor.extract("test", fragment);
        assertTrue(result.note().contains("Note text"), result.note());
    }
}
