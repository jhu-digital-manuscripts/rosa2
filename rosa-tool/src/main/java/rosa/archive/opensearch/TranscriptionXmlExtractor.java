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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Extracts the text of a TEI transcription XML fragment, sorted by the language it is in.
 *
 * <p>The Roman de la Rose transcriptions mix two languages. The manuscript text itself is
 * Old French: the verse lines ({@code <l>}), the rubrics ({@code <hi rend="rubric">}) and
 * the catchwords ({@code <fw>}). Everything the transcriber added is English: the
 * miniature descriptions ({@code <figure>}, with its {@code <head>} and
 * {@code <note type="character">} children) and the editorial notes ({@code <note>} of
 * any other type), even though critical notes often quote Old French readings from the
 * Lecoy edition.
 *
 * <p>Elements nest: a line may contain an initial ({@code <hi rend="init">}), abbreviation
 * expansions ({@code <expan>}), additions and deletions, a rubric, or an editorial note,
 * and a rubric may contain an initial or a note. Text is attributed to the innermost
 * element that selects a category, so the rest of a line continues as verse after an
 * initial or a note, and a note inside a line is English rather than verse.
 *
 * <p>Skipped entirely: line numbers ({@code <l n>}), Lecoy numbers
 * ({@code <milestone n>}), gaps and their {@code <desc>} (which hold the Lecoy numbers of
 * the missing lines), and scribal punctuation notes ({@code <note type="scribalPunc">}).
 */
final class TranscriptionXmlExtractor {

    private static final Logger log = Logger.getLogger(TranscriptionXmlExtractor.class.getName());

    private TranscriptionXmlExtractor() {}

    /**
     * The text of a transcription fragment, by category.
     *
     * <p>The verse of the page is one string, so that phrase queries can match across line
     * breaks. The other categories hold one entry per element, so that each rubric, note or
     * miniature description is indexed on its own.
     *
     * @param poetry        the verse lines (Old French), joined with spaces
     * @param rubrics       the rubrics (Old French)
     * @param catchphrases  the catchwords (Old French)
     * @param illustrations the miniature descriptions and character names (English)
     * @param notes         the editorial notes (English)
     */
    record Result(String poetry, List<String> rubrics, List<String> catchphrases,
                  List<String> illustrations, List<String> notes) {

        static final Result EMPTY = new Result("", List.of(), List.of(), List.of(), List.of());

        /** The Old French text: verse, then rubrics, then catchwords. */
        List<String> oldFrench() {
            List<String> out = new ArrayList<>();
            if (!poetry.isEmpty()) {
                out.add(poetry);
            }
            out.addAll(rubrics);
            out.addAll(catchphrases);
            return out;
        }

        /** The English text: miniature descriptions, then notes. */
        List<String> english() {
            List<String> out = new ArrayList<>(illustrations);
            out.addAll(notes);
            return out;
        }
    }

    /**
     * Parses a transcription XML fragment and extracts its text by category.
     *
     * @param name a name for the fragment, used in log messages
     * @param xml  the transcription XML fragment for a single page
     * @return the extracted text, or an empty result on blank input or parse failure
     */
    static Result extract(String name, String xml) {
        if (xml == null || xml.isBlank()) {
            return Result.EMPTY;
        }

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            TranscriptionHandler handler = new TranscriptionHandler();
            parser.parse(new InputSource(new StringReader(xml)), handler);
            return handler.getResult();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            log.log(Level.WARNING, "Failed to parse transcription XML fragment from " + name, e);
            return Result.EMPTY;
        }
    }

    /**
     * SAX handler that attributes text to the innermost enclosing element that selects a
     * category.
     *
     * <p>Every element pushes a frame. A frame either collects text into a buffer that is
     * added to a category when the element ends, continues the enclosing element's buffer,
     * or discards text. Text outside any collecting element is discarded.
     */
    private static class TranscriptionHandler extends DefaultHandler {

        /**
         * One open element.
         *
         * @param buffer where its text goes, or null to discard it
         * @param sink   the list its buffer is added to when the element ends, or null when the
         *               buffer belongs to an enclosing element
         */
        private record Frame(StringBuilder buffer, List<String> sink) {
            static final Frame DISCARD = new Frame(null, null);

            static Frame collect(List<String> sink) {
                return new Frame(new StringBuilder(), sink);
            }

            Frame inherit() {
                return new Frame(buffer, null);
            }
        }

        private final StringBuilder poetry = new StringBuilder();
        private final List<String> rubrics = new ArrayList<>();
        private final List<String> catchphrases = new ArrayList<>();
        private final List<String> illustrations = new ArrayList<>();
        private final List<String> notes = new ArrayList<>();

        private final Deque<Frame> frames = new ArrayDeque<>();

        TranscriptionHandler() {
            frames.push(Frame.DISCARD);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) {
            Frame parent = frames.peek();
            Frame frame = switch (qName) {
                case "l" -> {
                    poetry.append(' ');
                    yield new Frame(poetry, null);
                }
                case "hi" -> "rubric".equals(atts.getValue("rend")) ? Frame.collect(rubrics) : parent.inherit();
                case "fw" -> Frame.collect(catchphrases);
                case "figure" -> Frame.collect(illustrations);
                case "note" -> note(atts.getValue("type"), parent);
                case "gap", "teiHeader" -> Frame.DISCARD;
                default -> parent.inherit();
            };
            frames.push(frame);
        }

        private Frame note(String type, Frame parent) {
            if (type != null && type.startsWith("scribalPun")) {
                return Frame.DISCARD;
            }
            if ("character".equals(type)) {
                // Character names belong to the miniature they are listed under
                if (parent.sink() == illustrations) {
                    parent.buffer().append(' ');
                    return parent.inherit();
                }
                return Frame.collect(illustrations);
            }
            return Frame.collect(notes);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            Frame frame = frames.pop();
            if (frame.sink() != null) {
                String text = normalize(frame.buffer());
                if (!text.isEmpty()) {
                    frame.sink().add(text);
                }
            }
        }

        @Override
        public void characters(char[] text, int offset, int len) {
            StringBuilder buffer = frames.peek().buffer();
            if (buffer != null) {
                buffer.append(text, offset, len);
            }
        }

        Result getResult() {
            return new Result(normalize(poetry), List.copyOf(rubrics), List.copyOf(catchphrases),
                    List.copyOf(illustrations), List.copyOf(notes));
        }

        private static String normalize(StringBuilder sb) {
            return sb.toString().replaceAll("\\s+", " ").trim();
        }
    }
}
