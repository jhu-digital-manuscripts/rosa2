package rosa.iiif.presentation.core.extres;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

// Hard coded to grab people names from a  particular Perseus dictionary
// http://cts.perseids.org/read/pdlrefwk/viaf88890045/003/perseus-eng1
// 
// <div type="textpart" subtype="alphabetic_letter" n="A">
// <head>A</head>
// (Various entries under A)
// <div type="textpart" subtype="entry" xml:id="abaris-bio-1" n="abaris_1">
// <head><persName xml:lang="la"><surname full="yes">A'baris</surname></persName></head>
//
// URL: http://cts.perseids.org/read/pdlrefwk/viaf88890045/003/perseus-eng1/A.abaris_1

public class PerseusDictionary extends SimpleExternalResourceDb {
    private static final String BOOK_URL = "http://cts.perseids.org/read/pdlrefwk/viaf88890045/003/perseus-eng1/";
    private static final String BOOK_RES_PATH = "/viaf88890045.003.perseus-eng1.xml";

    private final String book_url;

    public PerseusDictionary() throws IOException {
        this(BOOK_URL, PerseusDictionary.class.getResourceAsStream(BOOK_RES_PATH));
    }

    public PerseusDictionary(String book_url, InputStream is) throws IOException {
        this.book_url = book_url;

        try {
            SAXParserFactory.newInstance().newSAXParser().parse(is, new SaxHandler());
        } catch (SAXException | ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    // Remove punctuation and trailing whitespace
    public String normalize(String s) {
        return s.replaceAll("\\p{Punct}+", "").toLowerCase().trim();
    }

    private class SaxHandler extends DefaultHandler {
        private String letter_id;
        private String entry_id;
        private String text;
        private StringBuilder name;

        public SaxHandler() {
            this.name = new StringBuilder();
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            text = new String(ch, start, length);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            if (qName.equals("div")) {
                String subtype = attributes.getValue("subtype");

                if (subtype != null) {
                    if (subtype.equals("alphabetic_letter")) {
                        letter_id = attributes.getValue("n");
                    } else if (subtype.equals("entry")) {
                        entry_id = attributes.getValue("n");
                    }
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equals("surname")) {
                name.append(text);
            } else if (qName.equals("addName")) {
                name.append(' ');
                name.append(text);
            } else if (qName.equals("persName")) {
                try {
                    add(name.toString(), new URI(book_url + letter_id + "." + entry_id));
                    name.setLength(0);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
