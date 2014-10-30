package rosa.archive.core.serialize;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import rosa.archive.model.BookText;
import rosa.archive.model.meta.BiblioData;
import rosa.archive.model.meta.MultilangMetadata;

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
 *
 */
public class MultilangMetadataSerializer implements Serializer<MultilangMetadata> {
    @Override
    public MultilangMetadata read(InputStream is, final List<String> errors) throws IOException {
        try {

            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            builder.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException e) throws SAXException {
                    // do nothing
                }

                @Override
                public void error(SAXParseException e) throws SAXException {
                    errors.add("[Error] (line=" + e.getLineNumber() + ", col=" + e.getColumnNumber()
                            + ") " + e.getMessage());
                }

                @Override
                public void fatalError(SAXParseException e) throws SAXException {
                    errors.add("[Fatal Error] (line=" + e.getLineNumber() + ", col=" + e.getColumnNumber()
                            + ") " + e.getMessage());
                }
            });

            Document doc = builder.parse(is);
            return buildMetadata(doc);

        } catch (ParserConfigurationException e) {
            throw new IOException("Failed to build Document.", e);
        } catch (SAXException e) {
            throw new IOException("Failed to parse input stream.", e);
        }
    }

    @Override
    public void write(MultilangMetadata object, OutputStream out) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    private MultilangMetadata buildMetadata(Document doc) {
        Element top = doc.getDocumentElement();

        MultilangMetadata metadata = new MultilangMetadata();

        metadata.setWidth(number("width", top));
        metadata.setHeight(number("height", top));
        metadata.setDimensionUnits(getAttribute("units", "dimensions", top));
        metadata.setNumberOfPages(number("totalPages", top));
        metadata.setNumberOfIllustrations(number("illustrations", top));
        metadata.setYearStart(number("startDate", top));
        metadata.setYearEnd(number("endDate", top));

        metadata.setBookTexts(getBookTexts(top));
        metadata.setBiblioDataMap(getBibliographies(top));

        return metadata;
    }

    private List<BookText> getBookTexts(Element parent) {
        List<BookText> textList = new ArrayList<>();

        NodeList list = parent.getElementsByTagName("texts");
        if (list.getLength() != 1 || list.item(0).getNodeType() != Node.ELEMENT_NODE) {
            return textList;
        }

        Element texts = (Element) list.item(0);
        for (Element textEl : getElementsInList("text", texts)) {
            BookText text = new BookText();

            text.setTextId(text("textId", textEl));
            text.setColumnsPerPage(number("columnsPerPage", textEl));
            text.setLeavesPerGathering(number("leavesPerGathering", textEl));
            text.setLinesPerColumn(number("linesPerColumn", textEl));
            text.setNumberOfIllustrations(number("illustrations", textEl));
            text.setNumberOfPages(number("pages", textEl));
            text.setFirstPage(getAttribute("start", "pages", textEl));
            text.setLastPage(getAttribute("end", "pages", textEl));
            text.setTitle(text("title", textEl));

            textList.add(text);
        }

        return textList;
    }

    private Map<String, BiblioData> getBibliographies(Element parent) {
        Map<String, BiblioData> map = new HashMap<>();

        NodeList list = parent.getElementsByTagName("bibliographies");
        if (list.getLength() != 1 || list.item(0).getNodeType() != Node.ELEMENT_NODE) {
            return map;
        }

        Element bibs = (Element) list.item(0);
        for (Element el : getElementsInList("bibliography", bibs)) {
            String lang = el.getAttribute("lang");

            BiblioData data = new BiblioData();

            data.setLanguage(lang);
            data.setTitle(text("title", el));
            data.setCommonName(text("commonName", el));
            data.setCurrentLocation(text("currentLocation", el));
            data.setDateLabel(text("dateLabel", el));
            data.setMaterial(text("material", el));
            data.setOrigin(text("origin", el));
            data.setRepository(text("repository", el));
            data.setShelfmark(text("shelfmark", el));
            data.setType(text("type", el));
            data.setDetails(getTextValues("detail", el).toArray(new String[0]));
            data.setAuthors(getTextValues("author", el).toArray(new String[0]));
            data.setNotes(getTextValues("note", el).toArray(new String[0]));

            map.put(lang, data);
        }

        return map;
    }

    private List<Element> getElementsInList(String elementName, Element parent) {
        List<Element> elements = new ArrayList<>();

        NodeList list = parent.getElementsByTagName(elementName);
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                elements.add((Element) node);
            }
        }

        return elements;
    }

    private List<String> getTextValues(String elementName, Element parent) {
        List<String> values = new ArrayList<>();

        for (Element el : getElementsInList(elementName, parent)) {
            values.add(el.getTextContent());
        }

        return values;
    }

    private String getAttribute(String attribute, String tag, Element parent) {
        List<Element> els = getElementsInList(tag, parent);

        if (els.size() > 0) {
            return els.get(0).getAttribute(attribute);
        } else {
            return "";
        }
    }

    private int getIntegerAttribute(String attribute, String tag, Element parent) {
        String text = getAttribute(attribute, tag, parent);

        if (text == null || text.equals("")) {
            return -1;
        }

        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private int number(String tagName, Element parent) {
        String text = text(tagName, parent);
        if (text == null) {
            return -1;
        }

        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String text(String tagName, Element parent) {
        List<Element> els = getElementsInList(tagName, parent);

        if (els.size() > 0) {
            return els.get(0).getTextContent();
        } else {
            return "";
        }
    }
}
