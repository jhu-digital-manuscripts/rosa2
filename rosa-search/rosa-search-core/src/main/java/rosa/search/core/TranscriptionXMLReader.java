package rosa.search.core;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A SAX content handler that will parse Roman de la Rose XML transcription
 * fragments to pull important data. Primarily used when indexing these
 * transcriptions. As this handler parses XML, it will record text content
 * for various categories such as poetry, lecoy, illustrations, etc which
 * can be retrieved when parsing is complete.
 *
 * The stored data is maintained between parsing different documents or
 * cleared using the {@link #clear()} method if the data should not be saved.
 */
public class TranscriptionXMLReader extends DefaultHandler {

    private StringBuffer poetry;
    private StringBuffer line;
    private StringBuffer lecoy;
    private StringBuffer rubric;
    private StringBuffer catchphrase;
    private StringBuffer illus;
    private StringBuffer note;

    private StringBuffer current = null;

    public TranscriptionXMLReader() {
        poetry = new StringBuffer();
        line = new StringBuffer();
        lecoy = new StringBuffer();
        rubric = new StringBuffer();
        catchphrase = new StringBuffer();
        illus = new StringBuffer();
        note = new StringBuffer();
    }

    /**
     * Clear any saved text data generated parsing a document.
     */
    public void clear() {
        poetry.delete(0, poetry.length());
        line.delete(0, line.length());
        lecoy.delete(0, lecoy.length());
        rubric.delete(0, rubric.length());
        catchphrase.delete(0, catchphrase.length());
        illus.delete(0, illus.length());
        note.delete(0, note.length());
    }

    public String getPoetry() {
        return poetry.toString();
    }

    public String getLine() {
        return line.toString();
    }

    public String getLecoy() {
        return lecoy.toString();
    }

    public String getRubric() {
        return rubric.toString();
    }

    public boolean hasRubric() {
        return rubric.length() > 0;
    }

    public String getCatchphrase() {
        return catchphrase.toString();
    }

    public boolean hasCatchphrase() {
        return catchphrase.length() > 0;
    }

    public String getIllustration() {
        return illus.toString();
    }

    public boolean hasIllus() {
        return illus.length() > 0;
    }

    public String getNote() {
        return note.toString();
    }

    public boolean hasNote() {
        return note.length() > 0;
    }

    @Override
    public void startDocument() throws SAXException {
        clear();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        String val;

        switch (qName) {
            case "note":
                val = getValue(atts, "type");

                if (val == null || val.equals("scribalPun")) {
                    break;
                }


                if (val.equals("character")) {
                    illus.append(' ');
                    current = illus;
                } else {
                    current = note;
                }

                break;
            case "figure":
                illus.append(' ');
                current = illus;
                break;
            case "l":
                line.append(' ');
                line.append(getValue(atts, "n"));
                poetry.append(' ');
                current = poetry;
                break;
            case "milestone":
                lecoy.append(' ');
                lecoy.append(getValue(atts, "n"));
                break;
            case "hi":
                val = getValue(atts, "rend");

                if (val != null && val.equals("rubric")) {
                    rubric.append(' ');
                    current = rubric;
                }

                break;
            case "fw":
                catchphrase.append(' ');
                current = catchphrase;
                break;
            case "head":
            case "lg":
            case "expan":
            case "add":
            case "del":
            case "rdg":
            case "app":
            case "cb":
            case "pb":
            case "div":
            case "gap":
            case "desc":
                break;
            default:
                System.err.println("Not handled " + qName);
                break;
        }
    }

    @Override
    public void characters(char[] text, int offset, int len)
            throws SAXException {
        if (current != null) {
            current.append(text, offset, len);
        }
    }

    private static String getValue(Attributes atts, String name) {
        for (int i = 0; i < atts.getLength(); i++) {
            if (atts.getQName(i).equals(name)) {
                return atts.getValue(i);
            }
        }

        return null;
    }

}
