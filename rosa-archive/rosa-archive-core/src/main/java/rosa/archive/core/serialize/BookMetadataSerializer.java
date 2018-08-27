package rosa.archive.core.serialize;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import rosa.archive.core.util.XMLUtil;
import rosa.archive.model.BiblioData;
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
 *
 */
public class BookMetadataSerializer implements Serializer<BookMetadata> {
    @Override
    public BookMetadata read(InputStream is, final List<String> errors) throws IOException {
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
    public void write(BookMetadata metadata, OutputStream out) throws IOException {
        Document doc = XMLUtil.newDocument();

        Element root = doc.createElement("book");
        doc.appendChild(root);

        Element license  = doc.createElement("license");
        root.appendChild(license);

        valueElement("url", metadata.getLicenseUrl() == null ? "" : metadata.getLicenseUrl(), license, doc);
        valueElement("logo", metadata.getLicenseLogo() == null ? "" : metadata.getLicenseLogo(), license, doc);

        valueElement("illustrations", metadata.getNumberOfIllustrations(), root, doc);
        valueElement("totalPages", metadata.getNumberOfPages(), root, doc);

        Element dimensions = doc.createElement("dimensions");
        root.appendChild(dimensions);
        dimensions.setAttribute("units", metadata.getDimensionUnits());
        valueElement("width", metadata.getWidth(), dimensions, doc);
        valueElement("height", metadata.getHeight(), dimensions, doc);

        Element dates = doc.createElement("dates");
        root.appendChild(dates);
        valueElement("startDate", metadata.getYearStart(), dates, doc);
        valueElement("endDate", metadata.getYearEnd(), dates, doc);

        Element texts = doc.createElement("texts");
        root.appendChild(texts);

        for (BookText t : metadata.getBookTexts()) {
            Element text = doc.createElement("text");
            texts.appendChild(text);

            valueElement("language", t.getLanguage(), text, doc);
            valueElement("title", t.getTitle(), text, doc);

            Element pages = valueElement("pages", t.getNumberOfPages(), text, doc);
            pages.setAttribute("end", t.getLastPage());
            pages.setAttribute("start", t.getFirstPage());

            valueElement("illustrations", t.getNumberOfIllustrations(), text, doc);
            valueElement("linesPerColumn", t.getLinesPerColumn(), text, doc);
            valueElement("leavesPerGathering", t.getLeavesPerGathering(), text, doc);
            valueElement("columnsPerPage", t.getColumnsPerPage(), text, doc);
        }

        Element bibs = doc.createElement("bibliographies");
        root.appendChild(bibs);
        for (String lang : metadata.getBiblioDataMap().keySet()) {
            BiblioData data = metadata.getBiblioDataMap().get(lang);

            Element bib = doc.createElement("bibliography");
            bibs.appendChild(bib);
            bib.setAttribute("lang", lang);

            valueElement("title", data.getTitle(), bib, doc);
            valueElement("commonName", data.getCommonName(), bib, doc);
            valueElement("dateLabel", data.getDateLabel(), bib, doc);
            valueElement("type", data.getType(), bib, doc);
            valueElement("material", data.getMaterial(), bib, doc);
            valueElement("origin", data.getOrigin(), bib, doc);
            valueElement("currentLocation", data.getCurrentLocation(), bib, doc);
            valueElement("repository", data.getRepository(), bib, doc);
            valueElement("shelfmark", data.getShelfmark(), bib, doc);

            for (String author : data.getAuthors()) {
                valueElement("author", author, bib, doc);
            }
            for (String detail : data.getDetails()) {
                valueElement("detail", detail, bib, doc);
            }
            for (String note : data.getNotes()) {
                valueElement("note", note, bib, doc);
            }
        }

        XMLUtil.write(doc, out, false);
    }

    private BookMetadata buildMetadata(Document doc) {
        Element top = doc.getDocumentElement();

        BookMetadata metadata = new BookMetadata();

        metadata.setWidth(number("width", top));
        metadata.setHeight(number("height", top));
        metadata.setDimensionUnits(getAttribute("units", "dimensions", top));
        metadata.setNumberOfPages(number("totalPages", top));
        metadata.setNumberOfIllustrations(number("illustrations", top));
        metadata.setYearStart(number("startDate", top));
        metadata.setYearEnd(number("endDate", top));

        metadata.setBookTexts(getBookTexts(top));
        metadata.setBiblioDataMap(getBibliographies(top));

        NodeList licenseList = top.getElementsByTagName("license");
        if (licenseList.getLength() == 1 && licenseList.item(0).getNodeType() == Node.ELEMENT_NODE) {
            Element licenseElement = (Element) licenseList.item(0);

            String url = text("url", licenseElement);
            metadata.setLicenseUrl(url.equals("") ? null : url);

            String logo = text("logo", licenseElement);
            metadata.setLicenseLogo(logo.equals("") ? null : logo);
        }

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

            text.setColumnsPerPage(number("columnsPerPage", textEl));
            text.setLeavesPerGathering(number("leavesPerGathering", textEl));
            text.setLinesPerColumn(number("linesPerColumn", textEl));
            text.setNumberOfIllustrations(number("illustrations", textEl));
            text.setNumberOfPages(number("pages", textEl));
            text.setFirstPage(getAttribute("start", "pages", textEl));
            text.setLastPage(getAttribute("end", "pages", textEl));
            text.setTitle(text("title", textEl));
            text.setLanguage(text("language", textEl));

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

    private Element valueElement(String tagName, String value, Element parent, Document doc) {
        Element el = doc.createElement(tagName);
        el.setTextContent(value);
        parent.appendChild(el);

        return el;
    }

    private Element valueElement(String tagName, int value, Element parent, Document doc) {
        Element el = doc.createElement(tagName);
        el.setTextContent(String.valueOf(value));
        parent.appendChild(el);

        return el;
    }

    @Override
    public Class<BookMetadata> getObjectType() {
        return BookMetadata.class;
    }
}
