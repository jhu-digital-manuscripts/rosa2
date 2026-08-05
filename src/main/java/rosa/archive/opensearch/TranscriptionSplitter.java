package rosa.archive.opensearch;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits a single TEI transcription XML document into per-page fragments.
 *
 * <p>The transcription XML uses {@code <pb n="..."/>} elements to mark page boundaries.
 * This class parses the XML using SAX and produces a map from normalized page name
 * (e.g., {@code "001r"}, {@code "135v"}) to the XML fragment for that page.
 *
 * <p>Page names are normalized to a 3-digit zero-padded format to match the convention
 * used by image identifiers in the archive.
 */
final class TranscriptionSplitter {
    private static final Logger log = Logger.getLogger(TranscriptionSplitter.class.getName());

    private TranscriptionSplitter() {}

    /**
     * Splits transcription XML into per-page fragments.
     *
     * @param xml the full transcription XML for a book
     * @return a map from normalized page name to XML fragment, or an empty map if
     *         the input is null/empty or parsing fails
     */
    static Map<String, String> split(String xml) {
        if (xml == null || xml.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            PageSplitHandler handler = new PageSplitHandler();
            parser.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), handler);
            return handler.getPageMap();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            log.log(Level.SEVERE, "Failed to parse transcription XML.", e);
            return Collections.emptyMap();
        }
    }

    /**
     * Normalizes a short image name (like {@code "1r"} or {@code "135r"}) to a 3-digit
     * zero-padded format (like {@code "001r"} or {@code "135r"}) matching the keys
     * produced by the splitter.
     *
     * @param imageName the short image name from {@link rosa.archive.model.BookImage#getName()}
     * @return the normalized page name, or the original name if it cannot be parsed
     */
    static String normalizePageName(String imageName) {
        if (imageName == null || imageName.isEmpty()) {
            return imageName;
        }
        // Strip leading/trailing whitespace
        String name = imageName.trim();
        // Match pattern: optional prefix letters, digits, then r/v suffix
        Matcher m = PAGE_PATTERN.matcher(name);
        if (m.matches()) {
            String prefix = m.group(1);
            int number = Integer.parseInt(m.group(2));
            String suffix = m.group(3);
            return prefix + String.format("%03d", number) + suffix;
        }
        return name;
    }

    private static final Pattern PAGE_PATTERN = Pattern.compile("^([a-zA-Z]*)(\\d+)(r|v)$");

    /**
     * SAX handler that splits TEI transcription XML at {@code <pb>} elements.
     */
    private static class PageSplitHandler extends DefaultHandler {
        private final Pattern pagePattern = Pattern.compile("^([a-zA-Z]*)(\\d+)(r|v)$");
        private final Map<String, String> pageMap = new HashMap<>();

        private String currentPage;
        private StringBuilder currentFragment;
        private boolean inLG = false;
        private boolean inHeader = false;

        @Override
        public void endDocument() throws SAXException {
            if (currentFragment != null && currentPage != null) {
                String frag = currentFragment.toString().trim();
                if (!frag.endsWith("</div>")) {
                    frag += "</div>";
                }
                pageMap.put(currentPage, frag);
            }
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (inHeader || qName == null || qName.isEmpty()) {
                return;
            }
            switch (qName) {
                case "pb" -> {
                    // End current fragment and store it
                    if (currentFragment != null && currentPage != null) {
                        if (inLG) {
                            currentFragment.append("</lg>");
                        }
                        currentFragment.append("</div>");
                        pageMap.put(currentPage, currentFragment.toString().replaceAll("\n", ""));
                    }
                    // Start new fragment
                    String pageAttr = attributes.getValue("n");
                    Matcher m = pagePattern.matcher(pageAttr != null ? pageAttr : "");
                    if (m.find()) {
                        currentPage = m.group(1)
                                + String.format("%03d", Integer.parseInt(m.group(2)))
                                + m.group(3);
                    } else {
                        currentPage = pageAttr;
                    }

                    // Handle duplicate page entries (multiple <pb> for same page with columns)
                    if (pageMap.containsKey(currentPage)) {
                        String previous = pageMap.get(currentPage);
                        if (previous.endsWith("</div>")) {
                            previous = previous.substring(0, previous.length() - 6);
                        }
                        currentFragment = new StringBuilder(previous);
                    } else {
                        currentFragment = new StringBuilder("<div type=\"ms\">");
                    }

                    currentFragment.append("<pb n=\"").append(currentPage).append("\"/>");
                    if (inLG) {
                        currentFragment.append("<lg type=\"couplet\">");
                    }
                }
                case "teiHeader" -> inHeader = true;
                case "", "text", "body" -> { /* skip structural wrappers */ }
                case "lg" -> {
                    inLG = true;
                    if (currentFragment != null) {
                        currentFragment.append("<").append(qName);
                        writeAttributes(attributes);
                        currentFragment.append('>');
                    }
                }
                default -> {
                    if (currentFragment != null) {
                        currentFragment.append("<").append(qName);
                        writeAttributes(attributes);
                        currentFragment.append('>');
                    }
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            switch (qName) {
                case "pb", "", "TEI", "text", "body" -> { /* skip */ }
                case "teiHeader" -> inHeader = false;
                case "lg" -> {
                    inLG = false;
                    if (currentFragment != null) {
                        currentFragment.append("</").append(qName).append('>');
                    }
                }
                default -> {
                    if (currentFragment != null) {
                        currentFragment.append("</").append(qName).append('>');
                    }
                }
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (currentFragment != null) {
                for (int i = 0; i < length; i++) {
                    currentFragment.append(ch[start + i]);
                }
            }
        }

        Map<String, String> getPageMap() {
            return pageMap;
        }

        private void writeAttributes(Attributes attributes) {
            for (int i = 0; i < attributes.getLength(); i++) {
                currentFragment.append(' ')
                        .append(attributes.getQName(i))
                        .append("=\"")
                        .append(attributes.getValue(i))
                        .append('"');
            }
        }
    }
}
