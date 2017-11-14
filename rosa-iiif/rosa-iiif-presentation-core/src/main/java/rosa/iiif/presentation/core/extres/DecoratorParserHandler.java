package rosa.iiif.presentation.core.extres;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class DecoratorParserHandler extends DefaultHandler {
    private XMLStreamWriter out;

    public DecoratorParserHandler(XMLStreamWriter out) {
        this.out = out;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equalsIgnoreCase("a")) {
            try {
                out.writeStartElement(qName);
                copyAttributes(attributes);
            } catch (XMLStreamException e) {}
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equalsIgnoreCase("a")) {
            try {
                out.writeEndElement();
            } catch (XMLStreamException e) {}
        }
    }

    @Override
    public void characters(char ch[], int start, int length) throws SAXException {
        try {
            out.writeCharacters(ch, start, length);
        } catch (XMLStreamException e) {}
    }

    private void copyAttributes(Attributes attributes) throws XMLStreamException {
        for (int i = 0; i < attributes.getLength(); i++) {
            out.writeAttribute(
                    attributes.getQName(i),
                    attributes.getValue(i)
            );
        }
    }
}
