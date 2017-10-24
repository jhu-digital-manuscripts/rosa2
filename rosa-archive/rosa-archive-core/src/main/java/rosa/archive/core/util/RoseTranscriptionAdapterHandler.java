package rosa.archive.core.util;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RoseTranscriptionAdapterHandler extends DefaultHandler {
    private static final Logger logger = Logger.getLogger("RoseTranscriptionAdapterHandler");

    private static final Set<String> skippedElements = new HashSet<>(Arrays.asList("l", "lg", "figure", "head", "div"));

    private ByteArrayOutputStream output;
    private XMLStreamWriter writer;

    private boolean inNote = false;

    public RoseTranscriptionAdapterHandler() {
        output = new ByteArrayOutputStream();
        try {
            writer = XMLOutputFactory.newInstance().createXMLStreamWriter(output);
            writer.writeStartElement("div");
        } catch (XMLStreamException e) {
            logger.log(Level.SEVERE, "Failed to create XML writer.");
            writer = null;
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (writer == null) {
            return;
        }

        try {
            switch (qName) {
                case "pb":
                    writer.writeStartElement("h3");
                    writer.writeCharacters(attributes.getValue("n"));
                    break;
                case "cb":
                    writer.writeStartElement("p");
                    writer.writeCharacters(attributes.getValue("n"));
                    break;
                case "expan":
                    writer.writeStartElement("span");
                    writer.writeAttribute("class", "italic");
                    break;
                case "hi":
                    writer.writeStartElement("span");
                    writer.writeAttribute("class", attributes.getValue("rend"));
                    break;
                case "note":
                    if ("critical".equals(attributes.getValue("type"))) {
                        inNote = true;
                        writer.writeStartElement("a");
                        writer.writeAttribute("href", "#");
                        writer.writeAttribute("class", "Tooltip");
                        writer.writeAttribute("onclick", "return false;");
                        writer.writeCharacters("*");
                        writer.writeStartElement("span");
                    }
                    break;
                case "milestone":
                    writer.writeStartElement("span");
                    writer.writeAttribute("class", "Lecoy");
                    writer.writeCharacters("L" + attributes.getValue("n"));
                    break;
                case "l":
                    writer.writeEmptyElement("br");
                default:
                    break;
            }
        } catch (XMLStreamException e) {
            throw new SAXException(e);
        }

    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (writer == null) {
            return;
        } else if (skipElement(qName)) {
            return;
        }

        try {
            if (inNote) {
                inNote = false;
                writer.writeEndElement();
            }
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new SAXException(e);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (writer == null) {
            return;
        }

        try {
            writer.writeCharacters(ch, start, length);
        } catch (XMLStreamException e) {
            throw new SAXException(e);
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


    public String getAdaptedString() {
        if (writer == null) {
            return "";
        }
        try {
            writer.writeEndElement();
            return output.toString("UTF-8");
        } catch (UnsupportedEncodingException | XMLStreamException e) {
            logger.log(Level.WARNING, "Failed to get result after adapting Rose transcription TEI to HTML.");
            return "";
        }
    }

    private boolean skipElement(String element) {
        return skippedElements.contains(element) || ("note".equals(element) && !inNote);
    }

}
