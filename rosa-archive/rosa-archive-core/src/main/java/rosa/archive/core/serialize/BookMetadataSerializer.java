package rosa.archive.core.serialize;

import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import rosa.archive.core.config.AppConfig;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.BookText;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @see rosa.archive.model.BookMetadata
 */
public class BookMetadataSerializer implements Serializer<BookMetadata> {

    private AppConfig config;

    @Inject
    BookMetadataSerializer(AppConfig config) {
        this.config = config;
    }

    @Override
    public BookMetadata read(InputStream is, List<String> errors) throws IOException {

        try {

            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(is);
            return buildMetadata(doc);

        } catch (ParserConfigurationException e) {
            String reason = "Failed to build Document.";
            errors.add(reason);
            throw new IOException(reason, e);
        } catch (SAXException e) {
            String reason = "Failed to parse input stream.";
            errors.add(reason);
            throw new IOException(reason, e);
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

        metadata.setDate(getString(top, config.getMetadataDateTag()));
        metadata.setCurrentLocation(getString(top, config.getMetadataCurrentLocationTag()));
        metadata.setRepository(getString(top, config.getMetadataRepositoryTag()));

        metadata.setShelfmark(getString(top, "shelfmark"));
        if (StringUtils.isBlank(metadata.getShelfmark())) {
            metadata.setShelfmark(getString(top, config.getMetadataShelfmarkTag()));
        }

        metadata.setOrigin(getString(top, config.getMetadataOriginTag()));
        metadata.setWidth(getInteger(top, config.getMetadataWidthTag()));
        metadata.setHeight(getInteger(top, config.getMetadataHeightTag()));
        metadata.setNumberOfIllustrations(getInteger(top, config.getMetadataNumIllustrationsTag()));
        metadata.setCommonName(getString(top, config.getMetadataCommonNameTag()));
        metadata.setMaterial(getString(top, config.getMetadataMaterialTag()));
        metadata.setType(getString(top, config.getMetadataTypeTag()));
        metadata.setDimensions((metadata.getWidth() == -1 || metadata.getHeight() == -1)
                ? "" : metadata.getWidth() + "x" + metadata.getHeight() + "mm");

        NodeList measureElement = top.getElementsByTagName(config.getMetadataMeasureTag());
        if (measureElement.getLength() > 0) {
            Element numPages = (Element) measureElement.item(0);
            metadata.setNumberOfPages(getIntegerQuietly(numPages.getAttribute(config.getMetadataNumPagesTag())));
        }

        NodeList dates = top.getElementsByTagName(config.getMetadataDateTag());
        if (dates.getLength() > 0) {
            Element date = (Element) dates.item(0);

            try {
                metadata.setYearStart(Integer.parseInt(date.getAttribute(config.getMetadataYearStartTag())));
                metadata.setYearEnd(Integer.parseInt(date.getAttribute(config.getMetadataYearEndTag())));
            } catch (NumberFormatException e) {
                metadata.setYearStart(-1);
                metadata.setYearEnd(-1);
            }
        }

        List<BookText> texts = new ArrayList<>();
        NodeList nodes = doc.getElementsByTagName(config.getMetadataTextsTag());

        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            BookText text = new BookText();

            text.setLinesPerColumn(getInteger(el, config.getMetadataTextsLinesPerColTag()));
            text.setColumnsPerPage(getInteger(el, config.getMetadataTextsColsPerPageTag()));
            text.setLeavesPerGathering(getInteger(el, config.getMetadataTextsLeavesPerGatheringTag()));
            text.setNumberOfIllustrations(getInteger(el, config.getMetadataNumIllustrationsTag()));
            text.setNumberOfPages(getInteger(el, config.getMetadataTextsNumPagesTag()));
            text.setId(getString(el, config.getMetadataTextsIdTag()));
            text.setTitle(getString(el, config.getMetadataTextsTitleTag()));

            NodeList locii = el.getElementsByTagName(config.getMetadataTextsLocusTag());
            if (locii.getLength() > 0) {
                Element range = (Element) locii.item(0);

                text.setFirstPage(range.getAttribute(config.getMetadataTextsFirstPageTag()));
                text.setLastPage(range.getAttribute(config.getMetadataTextsLastPageTag()));
            }
            texts.add(text);
        }

        metadata.setTexts(texts.toArray(new BookText[texts.size()]));

        return metadata;
    }
}
