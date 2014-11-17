package rosa.archive.core.serialize;

import com.google.inject.Inject;
import com.sun.org.apache.xml.internal.serializer.OutputPropertiesFactory;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import rosa.archive.core.config.AppConfig;
import rosa.archive.core.util.XMLUtil;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.BookText;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
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
    public void write(BookMetadata metadata, OutputStream out) throws IOException {
        Document doc = XMLUtil.newDocument();

        Element root = doc.createElement("TEI");
        root.setAttribute("xmlns", "http://www.tei-c.org/ns/1.0");
        root.setAttribute("version", "5.0");
        doc.appendChild(root);

        Element teiheader = doc.createElement("teiheader");
        root.appendChild(teiheader);

        Element sourceDesc = doc.createElement("sourceDesc");
        teiheader.appendChild(sourceDesc);

        // ------ bibl ------
        Element bibl = doc.createElement("bibl");
        sourceDesc.appendChild(bibl);

        // TODO pull Title out of the ID?
        Element title = doc.createElement(config.getMetadataTextsTitleTag());
        title.appendChild(doc.createTextNode(metadata.getTitle()));
        bibl.appendChild(title);

        // origin: <pubPlace>
        Element pubPlace = doc.createElement(config.getMetadataOriginTag());
        pubPlace.appendChild(doc.createTextNode(metadata.getOrigin()));
        bibl.appendChild(pubPlace);

        // <date>
        Element dateEl = doc.createElement(config.getMetadataDateTag());
        dateEl.setAttribute(config.getMetadataYearEndTag(), String.valueOf(metadata.getYearEnd()));
        dateEl.setAttribute(config.getMetadataYearStartTag(), String.valueOf(metadata.getYearStart()));
        dateEl.appendChild(doc.createTextNode(metadata.getDate()));
        bibl.appendChild(dateEl);

        // Notes: format, commonName, material, illustrations
        bibl.appendChild(note(config.getMetadataTypeTag(), metadata.getType(), doc));
        bibl.appendChild(note(config.getMetadataCommonNameTag(), metadata.getCommonName(), doc));
        bibl.appendChild(note(config.getMetadataMaterialTag(), metadata.getMaterial(), doc));
        bibl.appendChild(note(
                config.getMetadataNumIllustrationsTag(),
                String.valueOf(metadata.getNumberOfIllustrations()),
                doc
        ));

        // <extent>
        Element extentEl = doc.createElement("extent");
        bibl.appendChild(extentEl);

        //    <measure>
        Element measureEl = doc.createElement(config.getMetadataMeasureTag());
        extentEl.appendChild(measureEl);
        measureEl.setAttribute(config.getMetadataNumPagesTag(), String.valueOf(metadata.getNumberOfPages()));
        measureEl.setAttribute("unit", "folios");
        measureEl.appendChild(doc.createTextNode(metadata.getNumberOfPages() + " folios"));

        //    <dimensions>
        Element dimensionsEl = doc.createElement("dimensions");
        extentEl.appendChild(dimensionsEl);

        //        <height>TODO record dimension units in model!
        Element height = doc.createElement(config.getMetadataHeightTag());
        dimensionsEl.appendChild(height);
        height.setAttribute("unit", metadata.getDimensionUnits());
        height.appendChild(doc.createTextNode(String.valueOf(metadata.getHeight())));

        //        <width>
        Element width = doc.createElement(config.getMetadataWidthTag());
        dimensionsEl.appendChild(width);
        height.setAttribute("unit", metadata.getDimensionUnits());
        width.appendChild(doc.createTextNode(String.valueOf(metadata.getWidth())));

        // ------ msDesc ------
        Element msDesc = doc.createElement("msDesc");
        sourceDesc.appendChild(msDesc);

        //    <msIdentifier>
        Element msIdentifier = doc.createElement("msIdentifier");
        msDesc.appendChild(msIdentifier);

        //        <settlement>
        Element origin = doc.createElement(config.getMetadataOriginTag());
        msIdentifier.appendChild(origin);
        origin.appendChild(doc.createTextNode(metadata.getOrigin()));

        //        <repository>
        Element repository = doc.createElement(config.getMetadataRepositoryTag());
        msIdentifier.appendChild(repository);
        repository.appendChild(doc.createTextNode(metadata.getRepository()));

        //        <idno>
        Element shelfmark = doc.createElement(config.getMetadataShelfmarkTag());
        msIdentifier.appendChild(shelfmark);
        shelfmark.appendChild(doc.createTextNode(metadata.getShelfmark()));

        //    <msContents>
        Element msContents = doc.createElement("msContents");
        msDesc.appendChild(msContents);

        for (int i = 0; i < metadata.getTexts().length; i++) {
            BookText text = metadata.getTexts()[i];

        //        <msItems>
            Element msItem = doc.createElement("msItem");
            msContents.appendChild(msItem);
            msItem.setAttribute("n", String.valueOf(i));

            Element locus = doc.createElement(config.getMetadataTextsLocusTag());
            msItem.appendChild(locus);
            locus.setAttribute(config.getMetadataTextsFirstPageTag(), text.getFirstPage());
            locus.setAttribute(config.getMetadataTextsLastPageTag() , text.getLastPage() );
            locus.appendChild(doc.createTextNode(text.getFirstPage() + "-" + text.getLastPage()));

            // TODO title again...

            msItem.appendChild(note(
                    config.getMetadataTextsIdTag(),
                    text.getId(),
                    doc
            ));
            msItem.appendChild(note(
                    config.getMetadataTextsNumPagesTag(),
                    String.valueOf(text.getNumberOfPages()),
                    doc
            ));
            msItem.appendChild(note(
                    config.getMetadataNumIllustrationsTag(),
                    String.valueOf(text.getNumberOfIllustrations()),
                    doc
            ));
            msItem.appendChild(note(
                    config.getMetadataTextsLinesPerColTag(),
                    String.valueOf(text.getLinesPerColumn()),
                    doc
            ));
            msItem.appendChild(note(
                    config.getMetadataTextsLeavesPerGatheringTag(),
                    String.valueOf(text.getLeavesPerGathering()),
                    doc
            ));
            msItem.appendChild(note(
                    config.getMetadataTextsColsPerPageTag(),
                    String.valueOf(text.getColumnsPerPage()),
                    doc
            ));
        }

        XMLUtil.write(doc, out);
    }

    /**
     * <note type="{@param type}">{@param text}</note>
     * &lt;note type="{@param type}"&gt;{@param text}&lt;/note&gt;
     *
     * @param type type attribute
     * @param text note text
     * @param doc containing document
     * @return the note element
     */
    private Element note(String type, String text, Document doc) {
        Element note = doc.createElement("note");

        note.setAttribute("type", type);
        note.appendChild(doc.createTextNode(text));

        return note;
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

//        metadata.setTitle(getString(top, config.getMetadataTextsTitleTag()));
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
