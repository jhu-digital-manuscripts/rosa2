package rosa.archive.core.util;

import java.io.IOException;
import java.io.StringReader;
import java.util.function.Function;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


// TODO Use a HTML library

/**
 * Adapt transcriptions from Rose collection
 */
public class RoseTranscriptionAdapter {

    /**
     * Adapt transcription XML to HTML for display.
     *
     * @param xml string contents
     * @return HTML representations, a String per column in each page
     */
    public String toHtml(String xml, Function<String, String> text_to_html) {
        try {
            HtmlTransformer tr = new HtmlTransformer(text_to_html);
            SAXParserFactory.newInstance().newSAXParser().parse(new InputSource(new StringReader(xml)), tr);
            return tr.getHtml();
        } catch (SAXException | ParserConfigurationException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    // TODO lecoy numbers.
    
    private class HtmlTransformer extends DefaultHandler {
        private final StringBuilder html;
        private final Function<String, String> text_to_html;
        
        public HtmlTransformer(Function<String, String> text_to_html) {
            this.html = new StringBuilder();
            this.text_to_html = text_to_html;
        }
        
        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            String text = new String(ch, start, length);
            
            html.append(text_to_html.apply(text));
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            if (qName.equals("div")) {
                html.append("<div>");
            } else if (qName.equals("pb")) {
                html.append("<h3>" + attributes.getValue("n") + "</h3>");
            } else if (qName.equals("cb")) {
                html.append("<h5>Column " + attributes.getValue("n") + "</h5>");
            } else if (qName.equals("hi")) {
                html.append("<span class='" + attributes.getValue("rend") + "'>");
            } else if (qName.equals("expan")) {
                html.append("<span class='expan'>");
            } else if (qName.equals("l")) {
                html.append(attributes.getValue("n") + ". ");                
            } else if (qName.equals("figure")) {
                html.append("<p>");
            } else if (qName.equals("note")) {
                html.append(" (");                
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equals("div")) {
                html.append("</div>");
            } else if (qName.equals("hi") || qName.equals("expan")) {
                html.append("</span>");
            } else if (qName.equals("l")) {
                html.append("<br/>");
            } else if (qName.equals("figure")) {
                html.append("</p>");
            } else if (qName.equals("note")) {
                html.append(")");                
            }
        }
        
        public String getHtml() {
            return html.toString();
        }
    }

}
