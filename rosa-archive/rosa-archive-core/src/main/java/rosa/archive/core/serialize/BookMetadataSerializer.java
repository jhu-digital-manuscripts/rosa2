package rosa.archive.core.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import rosa.archive.core.util.XMLUtil;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.BookText;

/**
 * @see rosa.archive.model.BookMetadata
 */
public class BookMetadataSerializer implements Serializer<BookMetadata> {
    private static final String MetadataDateTag = "date";
    private static final String MetadataCurrentLocationTag = "settlement";
    private static final String MetadataRepositoryTag = "repository";
    private static final String MetadataShelfmarkTag = "idno";
    private static final String MetadataOriginTag = "pubPlace";
    private static final String MetadataWidthTag = "width";
    private static final String MetadataHeightTag = "height";
    private static final String MetadataNumIllustrationsTag = "illustrations";
    private static final String MetadataCommonNameTag = "commonName";
    private static final String MetadataMaterialTag = "material";
    private static final String MetadataTypeTag = "format";
    private static final String MetadataMeasureTag = "measure";
    private static final String MetadataNumPagesTag = "quantity";
    private static final String MetadataYearStartTag = "notBefore";
    private static final String MetadataYearEndTag = "notAfter";
    private static final String MetadataTextsTag = "msItem";
    private static final String MetadataTextsLinesPerColTag = "linesPerColumn";
    private static final String MetadataTextsColsPerPageTag = "columnsPerFolio";
    private static final String MetadataTextsLeavesPerGatheringTag = "leavesPerGathering";
    private static final String MetadataTextsNumPagesTag = "folios";
    private static final String MetadataTextsIdTag = "textid";
    private static final String MetadataTextsTitleTag = "title";
    private static final String MetadataTextsLocusTag = "locus";
    private static final String MetadataTextsFirstPageTag = "from";
    private static final String MetadataTextsLastPageTag = "to";
       
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

        Element title = doc.createElement(MetadataTextsTitleTag);
        title.appendChild(doc.createTextNode(metadata.getTitle()));
        bibl.appendChild(title);

        // origin: <pubPlace>
        Element pubPlace = doc.createElement(MetadataOriginTag);
        pubPlace.appendChild(doc.createTextNode(metadata.getOrigin()));
        bibl.appendChild(pubPlace);

        // <date>
        Element dateEl = doc.createElement(MetadataDateTag);
        dateEl.setAttribute(MetadataYearEndTag, String.valueOf(metadata.getYearEnd()));
        dateEl.setAttribute(MetadataYearStartTag, String.valueOf(metadata.getYearStart()));
        dateEl.appendChild(doc.createTextNode(metadata.getDate()));
        bibl.appendChild(dateEl);

        // Notes: format, commonName, material, illustrations
        bibl.appendChild(note(MetadataTypeTag, metadata.getType(), doc));
        bibl.appendChild(note(MetadataCommonNameTag, metadata.getCommonName(), doc));
        bibl.appendChild(note(MetadataMaterialTag, metadata.getMaterial(), doc));
        bibl.appendChild(note(MetadataNumIllustrationsTag,
                String.valueOf(metadata.getNumberOfIllustrations()), doc));

        // <extent>
        Element extentEl = doc.createElement("extent");
        bibl.appendChild(extentEl);

        // <measure>
        Element measureEl = doc.createElement(MetadataMeasureTag);
        extentEl.appendChild(measureEl);
        measureEl.setAttribute(MetadataNumPagesTag, String.valueOf(metadata.getNumberOfPages()));
        measureEl.setAttribute("unit", "folios");
        measureEl.appendChild(doc.createTextNode(metadata.getNumberOfPages() + " folios"));

        // <dimensions>
        Element dimensionsEl = doc.createElement("dimensions");
        extentEl.appendChild(dimensionsEl);

        Element height = doc.createElement(MetadataHeightTag);
        dimensionsEl.appendChild(height);
        height.setAttribute("unit", metadata.getDimensionUnits());
        height.appendChild(doc.createTextNode(String.valueOf(metadata.getHeight())));

        // <width>
        Element width = doc.createElement(MetadataWidthTag);
        dimensionsEl.appendChild(width);
        height.setAttribute("unit", metadata.getDimensionUnits());
        width.appendChild(doc.createTextNode(String.valueOf(metadata.getWidth())));

        // ------ msDesc ------
        Element msDesc = doc.createElement("msDesc");
        sourceDesc.appendChild(msDesc);

        // <msIdentifier>
        Element msIdentifier = doc.createElement("msIdentifier");
        msDesc.appendChild(msIdentifier);

        // <settlement>
        Element origin = doc.createElement(MetadataOriginTag);
        msIdentifier.appendChild(origin);
        origin.appendChild(doc.createTextNode(metadata.getOrigin()));

        // <repository>
        Element repository = doc.createElement(MetadataRepositoryTag);
        msIdentifier.appendChild(repository);
        repository.appendChild(doc.createTextNode(metadata.getRepository()));

        // <idno>
        Element shelfmark = doc.createElement(MetadataShelfmarkTag);
        msIdentifier.appendChild(shelfmark);
        shelfmark.appendChild(doc.createTextNode(metadata.getShelfmark()));

        // <msContents>
        Element msContents = doc.createElement("msContents");
        msDesc.appendChild(msContents);

        for (int i = 0; i < metadata.getTexts().length; i++) {
            BookText text = metadata.getTexts()[i];

            // <msItems>
            Element msItem = doc.createElement("msItem");
            msContents.appendChild(msItem);
            msItem.setAttribute("n", String.valueOf(i));

            Element locus = doc.createElement(MetadataTextsLocusTag);
            msItem.appendChild(locus);
            locus.setAttribute(MetadataTextsFirstPageTag, text.getFirstPage());
            locus.setAttribute(MetadataTextsLastPageTag, text.getLastPage());
            locus.appendChild(doc.createTextNode(text.getFirstPage() + "-" + text.getLastPage()));

            msItem.appendChild(note(MetadataTextsIdTag, text.getId(), doc));
            msItem.appendChild(note(MetadataTextsNumPagesTag, String.valueOf(text.getNumberOfPages()), doc));
            msItem.appendChild(note(MetadataNumIllustrationsTag,
                    String.valueOf(text.getNumberOfIllustrations()), doc));
            msItem.appendChild(note(MetadataTextsLinesPerColTag, String.valueOf(text.getLinesPerColumn()),
                    doc));
            msItem.appendChild(note(MetadataTextsLeavesPerGatheringTag,
                    String.valueOf(text.getLeavesPerGathering()), doc));
            msItem.appendChild(note(MetadataTextsColsPerPageTag, String.valueOf(text.getColumnsPerPage()),
                    doc));
        }

        XMLUtil.write(doc, out, false);
    }

    /**
     * Create a new &lt;note&gt; tag, with a 'type' attribute. The text
     * content of this new tag is set to 'text'.
     *
     * @param type
     *            type attribute
     * @param text
     *            note text
     * @param doc
     *            containing document
     * @return the note element
     */
    private Element note(String type, String text, Document doc) {
        Element note = doc.createElement("note");

        note.setAttribute("type", type);
        note.appendChild(doc.createTextNode(text));

        return note;
    }

    /**
     * Get the text value of the first element encountered with the specified
     * name. If no tags exist within the base element with the specified name,
     * then all <code>&lt;note&gt;</code> tags are searched. A
     * <code>&lt;note&gt;</code> tag is determined to match if 'name' matches
     * the 'type' attribute.
     *
     * If both of these searches fails to find any matches, a value of NULL is
     * returned.
     *
     * Example: method call: .firstElementValue(someElement, "commonName");
     *
     * If no <code>&lt;commonName&gt;</code> tags exist within <em>someElement</em>,
     * then this method will search for <code>&lt;note type="commonName"&gt;</code>.
     * The text content of this tag will be returned if it is found.
     * 
     * @param el
     *            search inside this element
     * @param name
     *            name of tag to look for
     * @return text value of the desired element. if multiple elements exist,
     *         than the first value is taken
     */
    private String firstElementValue(Element el, String name) {
        NodeList list = el.getElementsByTagName(name);

        // No XML tag with specified name.
        if (list.getLength() == 0) {
            // First check if there is a <note> tag with 'type' attribute equal
            // to specified name
            NodeList notes = el.getElementsByTagName("note");

            if (notes == null) {
                return null;
            }

            // in read() method, create a Map<String, String> (type attribute ->
            // textContent)?
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
     * a parsing error, instead it will return the value of -1. A parsing error
     * will happen if the input string is blank, or is not a number.
     * 
     * @param integer
     *            string to parse
     * @return integer equivalent of input string
     */
    private int getIntegerQuietly(String integer) {
        if (StringUtils.isBlank(integer)) {
            return -1;
        }

        try {
            return Integer.parseInt(integer.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * From an XML document, build the metadata object.
     * 
     * @param doc
     *            XML document
     * @return metadata
     */
    private BookMetadata buildMetadata(Document doc) {
        BookMetadata metadata = new BookMetadata();

        Element top = doc.getDocumentElement();

        metadata.setTitle(getString(top, MetadataTextsTitleTag));
        metadata.setDate(getString(top, MetadataDateTag));
        metadata.setCurrentLocation(getString(top, MetadataCurrentLocationTag));
        metadata.setRepository(getString(top, MetadataRepositoryTag));

        metadata.setShelfmark(getString(top, "shelfmark"));
        if (StringUtils.isBlank(metadata.getShelfmark())) {
            metadata.setShelfmark(getString(top, MetadataShelfmarkTag));
        }

        metadata.setOrigin(getString(top, MetadataOriginTag));
        metadata.setWidth(getInteger(top, MetadataWidthTag));
        metadata.setHeight(getInteger(top, MetadataHeightTag));
        metadata.setNumberOfIllustrations(getInteger(top, MetadataNumIllustrationsTag));
        metadata.setCommonName(getString(top, MetadataCommonNameTag));
        metadata.setMaterial(getString(top, MetadataMaterialTag));
        metadata.setType(getString(top, MetadataTypeTag));
        metadata.setDimensions((metadata.getWidth() == -1 || metadata.getHeight() == -1) ? "" : metadata.getWidth()
                + "x" + metadata.getHeight() + "mm");

        NodeList measureElement = top.getElementsByTagName(MetadataMeasureTag);
        if (measureElement.getLength() > 0) {
            Element numPages = (Element) measureElement.item(0);
            metadata.setNumberOfPages(getIntegerQuietly(numPages.getAttribute(MetadataNumPagesTag)));
        }

        NodeList dates = top.getElementsByTagName(MetadataDateTag);
        if (dates.getLength() > 0) {
            Element date = (Element) dates.item(0);

            try {
                metadata.setYearStart(Integer.parseInt(date.getAttribute(MetadataYearStartTag)));
                metadata.setYearEnd(Integer.parseInt(date.getAttribute(MetadataYearEndTag)));
            } catch (NumberFormatException e) {
                metadata.setYearStart(-1);
                metadata.setYearEnd(-1);
            }
        }

        List<BookText> texts = new ArrayList<>();
        NodeList nodes = doc.getElementsByTagName(MetadataTextsTag);

        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            BookText text = new BookText();

            text.setId(String.valueOf(i));
            text.setLinesPerColumn(getInteger(el, MetadataTextsLinesPerColTag));
            text.setColumnsPerPage(getInteger(el, MetadataTextsColsPerPageTag));
            text.setLeavesPerGathering(getInteger(el, MetadataTextsLeavesPerGatheringTag));
            text.setNumberOfIllustrations(getInteger(el, MetadataNumIllustrationsTag));
            text.setNumberOfPages(getInteger(el, MetadataTextsNumPagesTag));
            text.setTextId(getString(el, MetadataTextsIdTag));
            text.setTitle(getString(el, MetadataTextsTitleTag));

            NodeList locii = el.getElementsByTagName(MetadataTextsLocusTag);
            if (locii.getLength() > 0) {
                Element range = (Element) locii.item(0);

                text.setFirstPage(range.getAttribute(MetadataTextsFirstPageTag));
                text.setLastPage(range.getAttribute(MetadataTextsLastPageTag));
            }
            texts.add(text);
        }

        metadata.setTexts(texts.toArray(new BookText[texts.size()]));

        return metadata;
    }

    @Override
    public Class<BookMetadata> getObjectType() {
        return BookMetadata.class;
    }
}
