package rosa.iiif.presentation.core.util;

import rosa.archive.model.aor.Location;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.util.stream.Stream;

public class AnnotationLocationUtil {
    public static String locationToHtml(Location... locations) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(out);
            writeLocationAsHtml(writer, locations);

            return out.toString();
        } catch (XMLStreamException e) {
            return "";
        }
    }

    public static String locationToClass(Location location) {
        if (location == null) {
            return "";
        }
        switch (location) {
            default:
                return "";
            case HEAD:
                return "side-top ";
            case TAIL:
                return "side-bottom ";
            case LEFT_MARGIN:
                return "side-left ";
            case RIGHT_MARGIN:
                return "side-right ";
            case INTEXT:
                return "side-within ";
            case FULL_PAGE:
                return "full-page ";
        }
    }

    /**
     *
     * @param writer xml stream writer
     * @param locations list of zero or more locations on the page
     * @throws XMLStreamException .
     */
    public static void writeLocationAsHtml(XMLStreamWriter writer, Location ... locations) throws XMLStreamException {
        if (locations == null || locations.length == 0) {
            return;
        }

        StringBuilder styleClass = new StringBuilder("aor-icon ");

        // For each distinct location value, append appropriate CSS class
        Stream.of(locations)
                .distinct()
                .map(AnnotationLocationUtil::locationToClass)
                .forEach(styleClass::append);

        writer.writeStartElement("i");
        writer.writeAttribute("class", styleClass.toString());

        if (Stream.of(locations).anyMatch(loc -> loc != null && loc.equals(Location.INTEXT))) {
            addSimpleElement(writer, "i", null, "class", "inner");
        }

        writer.writeEndElement();
    }

    /**
     * Create an simple element that may have attributes and may have simple string content.
     * This element cannot have nested elements.
     *
     * @param writer xml stream writer
     * @param element element name
     * @param content simple String content
     * @param attrs array of attributes for the new element, always attribute label followed by attribute value
     *              IF PRESENT, attrs MUST have even number of elements
     */
    private static void addSimpleElement(XMLStreamWriter writer, String element, String content, String ... attrs)
            throws XMLStreamException {
        writer.writeStartElement(element);
        if (attrs != null && attrs.length % 2 == 0) {
            for (int i = 0; i < attrs.length - 1;) {
                writer.writeAttribute(attrs[i++], attrs[i++]);
            }
        }
        if (isNotEmpty(content)) {
            writer.writeCharacters(content);
        }
        writer.writeEndElement();
    }

    private static boolean isNotEmpty(String str) {
        return str != null && !str.isEmpty();
    }
}
