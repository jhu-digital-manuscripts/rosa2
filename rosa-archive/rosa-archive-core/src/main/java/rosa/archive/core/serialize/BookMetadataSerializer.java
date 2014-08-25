package rosa.archive.core.serialize;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.BookText;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @see rosa.archive.model.BookMetadata
 */
public class BookMetadataSerializer implements Serializer<BookMetadata> {

    BookMetadataSerializer() {  }

    @Override
    public BookMetadata read(InputStream is) throws IOException {

        try {

            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(is);
            return buildMetadata(doc);

        } catch (ParserConfigurationException | SAXException e) {
            // TODO
            return null;
        }
    }

    @Override
    public void write(BookMetadata object, OutputStream out) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Get the text value of the first element encountered with the specified name. If
     * no tags exist within the base element ({@param el}) with the name {@param name},
     * then all <code>&lt;note&gt;</code> tags are searched. A <code>&lt;note&gt;</code>
     * tag is determined to match if {@param name} matches the 'type' attribute.
     *
     * If both of these searches fails to find any matches, a value of NULL is returned.
     *
     * Example: method call: .firstElementValue(someElement, "commonName");
     *
     * If no <code>&lt;commonName&gt;</code> tags exist within <em>someElement</em>,
     * then this method will search for <code>&lt;note type="commonName"&gt;</code>.
     * The text content of this tag will be returned if it is found.
     *
     * @param el search inside this element
     * @param name name of tag to look for
     * @return
     *          text value of the desired element. if multiple elements exist, than the
     *          first value is taken
     */
    private String firstElementValue(Element el, String name) {
        NodeList list = el.getElementsByTagName(name);

        // No XML tag with specified name.
        if (list.getLength() == 0) {
            // First check if there is a <note> tag with 'type' attribute equal to specified name
            NodeList notes = el.getElementsByTagName("note");

            if (notes == null) {
                return null;
            }

            // in read() method, create a Map<String, String> (type attribute -> textContent)?
            for (int i = 0; i < notes.getLength(); i++) {
                Element note = (Element) notes.item(i);
                if (note.getAttribute("type").equals(name)) {
                    return note.getTextContent();
                }
            }

            return null;
        }

        return list.item(0).getTextContent();
    }

    private String getString(Element el, String name) {
        return firstElementValue(el, name);
    }

    private int getInteger(Element el, String name) {
        return getIntegerQuietly(getString(el, name));
    }

    /**
     * Parse a string as an integer. Will not throw an exception in the event of
     * a parsing error, instead it will return the value of -1.
     * A parsing error will happen if the input string is blank, or is not a number.
     *
     * @param integer string to parse
     * @return integer equivalent of input string
     */
    private int getIntegerQuietly(String integer) {
        if (StringUtils.isBlank(integer)) {
            return -1;
        }

        try {
            return Integer.parseInt(integer.trim());
        } catch (NumberFormatException e) {
            // TODO log not a number error!
            return -1;
        }
    }

    /**
     * From an XML document, build the metadata object.
     *
     * @param doc XML document
     * @return metadata
     */
    private BookMetadata buildMetadata(Document doc) {
        BookMetadata metadata = new BookMetadata();

        Element top = doc.getDocumentElement();

        metadata.setDate(getString(top, "date"));
        metadata.setCurrentLocation(getString(top, "settlement"));
        metadata.setRepository(getString(top, "repository"));

        metadata.setShelfmark(getString(top, "shelfmark"));
        if (StringUtils.isBlank(metadata.getShelfmark())) {
            metadata.setShelfmark(getString(top, "idno"));
        }

        metadata.setOrigin(getString(top, "pubPlace"));
        metadata.setWidth(getInteger(top, "width"));
        metadata.setHeight(getInteger(top, "height"));
        metadata.setNumberOfIllustrations(getInteger(top, "illustrations"));
        metadata.setCommonName(getString(top, "commonName"));
        metadata.setMaterial(getString(top, "material"));
        metadata.setType(getString(top, "format"));
        metadata.setDimensions((metadata.getWidth() == -1 || metadata.getHeight() == -1)
                ? "" : metadata.getWidth() + "x" + metadata.getHeight() + "mm");

        NodeList measureElement = top.getElementsByTagName("measure");
        if (measureElement.getLength() > 0) {
            Element numPages = (Element) measureElement.item(0);
            metadata.setNumberOfPages(getIntegerQuietly(numPages.getAttribute("quantity")));
        }

        NodeList dates = top.getElementsByTagName("date");
        if (dates.getLength() > 0) {
            Element date = (Element) dates.item(0);

            try {
                metadata.setYearStart(Integer.parseInt(date.getAttribute("notBefore")));
                metadata.setYearEnd(Integer.parseInt(date.getAttribute("notAfter")));
            } catch (NumberFormatException e) {
                metadata.setYearStart(-1);
                metadata.setYearEnd(-1);
            }
        }

        List<BookText> texts = new ArrayList<>();
        NodeList nodes = doc.getElementsByTagName("msItem");

        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            BookText text = new BookText();

            text.setLinesPerColumn(getInteger(el, "linesPerColumn"));
            text.setColumnsPerPage(getInteger(el, "columnsPerFolio"));
            text.setLeavesPerGathering(getInteger(el, "leavesPerGathering"));
            text.setNumberOfIllustrations(getInteger(el, "illustrations"));
            text.setNumberOfPages(getInteger(el, "folios"));
            text.setId(getString(el, "textid"));
            text.setTitle(getString(el, "title"));

            NodeList locii = el.getElementsByTagName("locus");
            if (locii.getLength() > 0) {
                Element range = (Element) locii.item(0);

                text.setFirstPage(range.getAttribute("from"));
                text.setLastPage(range.getAttribute("to"));
            }
            texts.add(text);
        }

        metadata.setTexts(texts.toArray(new BookText[texts.size()]));

        return metadata;
    }
}
