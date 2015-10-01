package rosa.archive.core.util;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SAXSplitter extends DefaultHandler {
    private Logger logger = Logger.getLogger(SAXSplitter.class.toString());
    private Pattern pagePattern = Pattern.compile("^([a-zA-Z]*)(\\d+)(r|v)$");

    private Map<String, String> pageMap = new HashMap<>();

    private String currentPage;
    private StringBuilder currentFragment;

    private boolean inLG = false;
    private boolean inHeader = false;

    @Override
    public void startDocument() throws SAXException {

    }

    @Override
    public void endDocument() throws SAXException {
        // Transcription XML SHOULD end with </div>. Add it if it does not.
        if (!currentFragment.toString().endsWith("</div>")) {
            currentFragment.append("</div>");
        }

        pageMap.put(currentPage, currentFragment.toString());
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (inHeader || qName == null || qName.isEmpty()) {
            return;
        }
        switch (qName) {
            case "pb":
                // Page designation
                // End current fragment and write to map
                if (currentFragment != null) {
                    currentFragment.append("</div>");
                    pageMap.put(currentPage, currentFragment.toString());
                }
                // Start new fragment, must account for <pb> inside <lg> tags
                Matcher m = pagePattern.matcher(attributes.getValue("n"));
                if (m.find()) {
                    currentPage = m.group(1)
                            + String.format("%03d", Integer.parseInt(m.group(2)))
                            + m.group(3);
                } else {
                    currentPage = attributes.getValue("n");
                }

                // TODO could have simply
                // Check if new page == old page, if true, continue using old
                // StringBuilder. Then overwrite old value.
                if (pageMap.containsKey(currentPage)) {
                    /*
                        It is possible that there is a <pb> page designation for every
                        <cb> column designation. In this case, a page fragment may already
                        exist in the pageMap. The next fragment should be added to that
                        pre-existing fragment, instead of overwriting it.
                     */
                    String previous = pageMap.get(currentPage);
                    if (previous.endsWith("</div>")) {
                        previous = previous.substring(0, previous.length() - 6);
                    }

                    currentFragment = new StringBuilder(previous);
                    if (inLG) {
                        currentFragment.append("</lg>");
                    }
                } else {
                    currentFragment = new StringBuilder("<div type=\"ms\">");
                }

                currentFragment.append("<pb n=\"");
                currentFragment.append(currentPage);
                currentFragment.append("\"/>");

                if (inLG) {
                    currentFragment.append("<lg type=\"couplet\">");
                    // TODO will trap <cb> inside <lg> in most cases...
                }

                break;
            case "":
            case "text":
            case "body":
                break;
            case "teiHeader":
                inHeader = true;
                break;
            case "lg":
                inLG = true;
            default:
                // Write tag to current fragment
                if (currentFragment != null) {
                    currentFragment.append("<");
                    currentFragment.append(qName);
                    writeAttributes(attributes);
                    currentFragment.append('>');
                }
                break;
        }
    }

    private void writeAttributes(Attributes attributes) {
        for (int i = 0; i < attributes.getLength(); i++) {
            currentFragment.append(' ');
            currentFragment.append(attributes.getQName(i));
            currentFragment.append("=\"");
            currentFragment.append(attributes.getValue(i));
            currentFragment.append("\" ");
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        switch (qName) {
            case "pb":
                break;
            case "":
            case "TEI":
            case "text":
            case "body":
                break;
            case "teiHeader":
                inHeader = false;
                break;
            case "lg":
                inLG = false;
            default:
                // Record end of element in current fragment
                if (currentFragment != null) {
                    currentFragment.append("</");
                    currentFragment.append(qName);
                    currentFragment.append('>');
                }
                break;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (currentFragment == null) {
            return;
        }
        // Record characters in current fragment
        for (int i = 0; i < length; i++) {
            currentFragment.append(String.valueOf(ch[i + start]));
        }
    }

    @Override
    public void warning(SAXParseException e) throws SAXException {
        logger.log(Level.WARNING, "[WARNING] (" + e.getPublicId() + ":" + e.getSystemId() + ") Error at " +
                "line " + e.getLineNumber() + ", col " + e.getColumnNumber(), e);
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
        logger.log(Level.SEVERE, "[ERROR] (" + e.getPublicId() + ":" + e.getSystemId() + ") Error at " +
                "line " + e.getLineNumber() + ", col " + e.getColumnNumber(), e);
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
        logger.log(Level.SEVERE, "[FATAL ERROR] (" + e.getPublicId() + ":" + e.getSystemId() + ") Error at " +
                "line " + e.getLineNumber() + ", col " + e.getColumnNumber(), e);
    }

    public Map<String, String> getPageMap() {
        return pageMap;
    }
}
