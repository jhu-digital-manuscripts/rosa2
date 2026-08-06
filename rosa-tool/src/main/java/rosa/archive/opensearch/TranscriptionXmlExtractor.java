package rosa.archive.opensearch;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Extracts structured text content from a TEI transcription XML fragment.
 *
 * <p>Parses poetry lines, rubrics, catchphrases, illustration notes, Lecoy line numbers,
 * line numbers, and general notes into separate strings for language-appropriate indexing.
 *
 * <p>Poetry, rubrics, and catchphrases are typically Old French. Illustration descriptions,
 * Lecoy numbers, line numbers, and notes are indexed as English.
 */
final class TranscriptionXmlExtractor {

    private static final Logger log = Logger.getLogger(TranscriptionXmlExtractor.class.getName());

    private TranscriptionXmlExtractor() {}

    /**
     * The result of extracting text from a transcription XML fragment.
     *
     * @param poetry      poem line text content (Old French)
     * @param rubric      rubric heading text (Old French)
     * @param catchphrase catchphrase text (Old French)
     * @param illustration illustration and character note text (English)
     * @param lecoy       Lecoy line number references (English)
     * @param line        line number references (English)
     * @param note        general note text (English)
     */
    record Result(String poetry, String rubric, String catchphrase,
                  String illustration, String lecoy, String line, String note) {}

    /**
     * Parses a transcription XML fragment and extracts categorized text content.
     *
     * @param xml the transcription XML fragment for a single page
     * @return the extraction result with categorized text, or empty fields on parse failure
     */
    static Result extract(String xml) {
        if (xml == null || xml.isBlank()) {
            return new Result("", "", "", "", "", "", "");
        }

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            TranscriptionHandler handler = new TranscriptionHandler();
            parser.parse(new InputSource(new StringReader(xml)), handler);
            return handler.getResult();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            log.log(Level.WARNING, "Failed to parse transcription XML fragment.", e);
            return new Result("", "", "", "", "", "", "");
        }
    }

    /**
     * SAX handler that categorizes text content from TEI transcription XML.
     */
    private static class TranscriptionHandler extends DefaultHandler {
        private final StringBuilder poetry = new StringBuilder();
        private final StringBuilder line = new StringBuilder();
        private final StringBuilder lecoy = new StringBuilder();
        private final StringBuilder rubric = new StringBuilder();
        private final StringBuilder catchphrase = new StringBuilder();
        private final StringBuilder illustration = new StringBuilder();
        private final StringBuilder note = new StringBuilder();

        private StringBuilder current = null;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            switch (qName) {
                case "note" -> {
                    String val = atts.getValue("type");
                    if (val == null || val.equals("scribalPun")) {
                        break;
                    }
                    if (val.equals("character")) {
                        illustration.append(' ');
                        current = illustration;
                    } else {
                        current = note;
                    }
                }
                case "figure" -> {
                    illustration.append(' ');
                    current = illustration;
                }
                case "l" -> {
                    line.append(' ');
                    String n = atts.getValue("n");
                    if (n != null) {
                        line.append(n);
                    }
                    poetry.append(' ');
                    current = poetry;
                }
                case "milestone" -> {
                    lecoy.append(' ');
                    String n = atts.getValue("n");
                    if (n != null) {
                        lecoy.append(n);
                    }
                }
                case "hi" -> {
                    String val = atts.getValue("rend");
                    if (val != null && val.equals("rubric")) {
                        rubric.append(' ');
                        current = rubric;
                    }
                }
                case "fw" -> {
                    catchphrase.append(' ');
                    current = catchphrase;
                }
                // Structural elements: don't change current target
                case "head", "lg", "expan", "add", "del", "rdg", "app", "cb", "pb", "div", "gap", "desc" -> {}
                default -> {}
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            switch (qName) {
                case "l", "note", "figure", "hi", "fw" -> current = null;
                default -> {}
            }
        }

        @Override
        public void characters(char[] text, int offset, int len) throws SAXException {
            if (current != null) {
                current.append(text, offset, len);
            }
        }

        Result getResult() {
            return new Result(
                    poetry.toString().trim(),
                    rubric.toString().trim(),
                    catchphrase.toString().trim(),
                    illustration.toString().trim(),
                    lecoy.toString().trim(),
                    line.toString().trim(),
                    note.toString().trim()
            );
        }
    }
}
