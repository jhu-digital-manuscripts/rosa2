package rosa.search.core;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

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
        clear();
    }

    public void clear() {
        poetry = new StringBuffer();
        line = new StringBuffer();
        lecoy = new StringBuffer();
        rubric = new StringBuffer();
        catchphrase = new StringBuffer();
        illus = new StringBuffer();
        note = new StringBuffer();

        current = new StringBuffer();
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
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        String val;

        switch (localName) {
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
                System.err.println("Not handled " + localName);
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
            if (atts.getLocalName(i).equals(name)) {
                return atts.getValue(i);
            }
        }

        return null;
    }
    
}
