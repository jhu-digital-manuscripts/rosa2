package rosa.archive.core.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import rosa.archive.model.BookMetadata;
import rosa.archive.model.BookText;

public class MetadataSerializer implements Serializer<Map<String, BookMetadata>> {
    @Override
    public Map<String, BookMetadata> read(InputStream is, List<String> errors) throws IOException {
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
    public void write(Map<String, BookMetadata> metadataMap, OutputStream out) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
//        Document doc = XMLUtil.newDocument();
//
//        Element root = doc.createElement("book");
//        doc.appendChild(root);
//
//        boolean first = true;
//        for (String lang : metadataMap.keySet()) {
//            BookMetadata metadata = metadataMap.get(lang);
//
//            if (first) {
//                first = false;
//
//                root.setAttribute("illustrations", String.valueOf(metadata.getNumberOfIllustrations()));
//                root.setAttribute("pages", String.valueOf(metadata.getNumberOfPages()));
//                // TODO title
//
//                Element dimensions = doc.createElement("dimensions");
//                root.appendChild(dimensions);
//                dimensions.setAttribute("unit", "mm");
//                dimensions.setAttribute("width", String.valueOf(metadata.getWidth()));
//                dimensions.setAttribute("height", String.valueOf(metadata.getHeight()));
//
//                Element texts = doc.createElement("texts");
//                root.appendChild(texts);
//
//                for (BookText text : metadata.getTexts()) {
//                    Element el = doc.createElement("text");
//                    texts.appendChild(el);
//
//                    el.setAttribute("id", text.getId());
//                    el.setAttribute("title", text.getTitle());
//                    el.setAttribute("start", text.getFirstPage());
//                    el.setAttribute("end", text.getLastPage());
//                    el.setAttribute("pages", String.valueOf(text.getNumberOfPages()));
//                    el.setAttribute("illustrations", String.valueOf(text.getNumberOfIllustrations()));
//                    el.setAttribute("linesPerColumn", String.valueOf(text.getLinesPerColumn()));
//                    el.setAttribute("leavesPerGathering", String.valueOf(text.getLeavesPerGathering()));
//                    el.setAttribute("columnsPerPage", String.valueOf(text.getColumnsPerPage()));
//                }
//            }
//
//            Element bibliography = doc.createElement("bibliography");
//            root.appendChild(bibliography);
//            bibliography.setAttribute("lang", lang);
//
//            Element date = doc.createElement("date");
//            bibliography.appendChild(date);
//            date.setAttribute("start", String.valueOf(metadata.getYearStart()));
//            date.setAttribute("end", String.valueOf(metadata.getYearEnd()));
//            date.appendChild(doc.createTextNode(metadata.getDate()));
//
//            Element type = doc.createElement("type");
//            bibliography.appendChild(type);
//            type.appendChild(doc.createTextNode(metadata.getType()));
//
//            Element commonName = doc.createElement("commonName");
//            bibliography.appendChild(commonName);
//            commonName.appendChild(doc.createTextNode(metadata.getCommonName()));
//
//            Element material = doc.createElement("material");
//            bibliography.appendChild(material);
//            material.appendChild(doc.createTextNode(metadata.getMaterial()));
//
//            Element origin = doc.createElement("origin");
//            bibliography.appendChild(origin);
//            origin.appendChild(doc.createTextNode(metadata.getOrigin()));
//
//            Element currentLocation = doc.createElement("currentLocation");
//            bibliography.appendChild(currentLocation);
//            currentLocation.appendChild(doc.createTextNode(metadata.getCurrentLocation()));
//
//            Element repository = doc.createElement("repository");
//            bibliography.appendChild(repository);
//            repository.appendChild(doc.createTextNode(metadata.getRepository()));
//
//            Element shelfmark = doc.createElement("shelfmark");
//            bibliography.appendChild(shelfmark);
//            shelfmark.appendChild(doc.createTextNode(metadata.getShelfmark()));
//
////            Element measure = doc.createElement("measure");
////            bibliography.appendChild(measure);
////            measure.appendChild(doc.createTextNode(metadata.get))
//        }
//
//        XMLUtil.write(doc, out);
    }

    private Map<String, BookMetadata> buildMetadata(Document doc) {
        Map<String, BookMetadata> metadataMap = new HashMap<>();

        Element top = doc.getDocumentElement();

        for (String lang : getLanguages(doc)) {
            BookMetadata metadata = new BookMetadata();

            metadata.setNumberOfIllustrations(Integer.parseInt(top.getAttribute("illustrations")));
            metadata.setNumberOfPages(Integer.parseInt(top.getAttribute("totalPages")));

            NodeList list = top.getChildNodes();
            for (int i = 0; i < list.getLength(); i++) {
                Node node = list.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                Element el = (Element) node;
                if (el.getNodeName().equals("dimensions")) {
                    metadata.setHeight(Integer.parseInt(el.getAttribute("height")));
                    metadata.setWidth(Integer.parseInt(el.getAttribute("width")));
                } else if (el.getNodeName().equals("dates")) {
                    metadata.setYearStart(getFirstElementInt("startDate", el));
                    metadata.setYearEnd(getFirstElementInt("endDate", el));
                    // TODO dateLabel
                } else if (el.getNodeName().equals("texts")) {
                    List<BookText> texts = handleBookTexts(el);
                    metadata.setTexts(texts.toArray(new BookText[texts.size()]));
                } else if (el.getNodeName().equals("bibliography")) {
                    handleBiblio(el, lang, metadata);
                }
//                else if (el.getNodeName().equals("bibliography") && el.getAttribute("lang").equals(lang)) {
//                    readBibInfo(el, metadata);
//                }
            }

            metadataMap.put(lang, metadata);
        }

        return metadataMap;
    }

    private String[] getLanguages(Document doc) {
        List<String> langs = new ArrayList<>();

        NodeList bibs = doc.getElementsByTagName("bibliography");
        for (int i = 0; i < bibs.getLength(); i++) {
            Node node = bibs.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element child = (Element) node;
            String lang = child.getAttribute("lang");

            if (lang != null && !lang.equals("")) {
                langs.add(lang);
            }
        }

        return langs.toArray(new String[langs.size()]);
    }

    private void handleBiblio(Element bibs, String lang, BookMetadata metadata) {
        NodeList list = bibs.getElementsByTagName("bibliography");
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element bib = (Element) node;
            if (!bib.getAttribute("lang").equals(lang)) {
                continue;
            }

            metadata.setTitle(getFirstElement("title", bib));
            metadata.setDate(getFirstElement("dateLabel", bib));
            metadata.setType(getFirstElement("type", bib));
            metadata.setCommonName(getFirstElement("commonName", bib));
            metadata.setMaterial(getFirstElement("material", bib));
            metadata.setOrigin(getFirstElement("origin", bib));
            metadata.setCurrentLocation(getFirstElement("currentLocation", bib));
            metadata.setRepository(getFirstElement("repository", bib));
            metadata.setShelfmark(getFirstElement("shelfmark", bib));
        }
    }

    private void readBibInfo(Element biblio, BookMetadata metadata) {
        NodeList children = biblio.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element el = (Element) child;

            String name = el.getNodeName();
            String content = el.getTextContent();
            switch (name) {
                case "type":
                    metadata.setType(content);
                    break;
                case "commonName":
                    metadata.setCommonName(content);
                    break;
                case "material":
                    metadata.setMaterial(content);
                    break;
                case "origin":
                    metadata.setOrigin(content);
                    break;
                case "currentLocation":
                    metadata.setCurrentLocation(content);
                    break;
                case "repository":
                    metadata.setRepository(content);
                    break;
                case "shelfmark":
                    metadata.setShelfmark(content);
                    break;
                case "measure":
                    break;
                case "date":
                    metadata.setDate(content);
                    metadata.setYearStart(Integer.parseInt(el.getAttribute("start")));
                    metadata.setYearEnd(Integer.parseInt(el.getAttribute("end")));
                    break;
                default:
                    break;
            }
        }
    }

    private List<BookText> handleBookTexts(Element el) {
        List<BookText> texts = new ArrayList<>();

        NodeList list = el.getElementsByTagName("text");
        for (int i = 0; i < list.getLength(); i++) {
            if (list.item(i).getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element item = (Element) list.item(i);
            BookText text = new BookText();

            text.setId(item.getAttribute("id"));
            text.setTitle(getFirstElement("title", item));
            text.setFirstPage(getAttribute("pages", "start", item));
            text.setLastPage(getAttribute("pages", "end", item));
//            text.setNumberOfPages();
            text.setNumberOfIllustrations(getFirstElementInt("illustrations", item));
            text.setLinesPerColumn(getFirstElementInt("linesPerColumn", item));
            text.setLeavesPerGathering(getFirstElementInt("leavesPerGathering", item));
            text.setColumnsPerPage(getFirstElementInt("columnsPerPage", item));

            texts.add(text);
        }

        return texts;
    }

    private Element getElement(String tagname, Element top) {
        NodeList list = top.getElementsByTagName(tagname);
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            return (Element) node;
        }

        return null;
    }

    private String getFirstElement(String tagname, Element top) {
        Element el = getElement(tagname, top);

        if (el == null) {
            return "";
        } else {
            return el.getTextContent();
        }
    }

    private int getFirstElementInt(String tagname, Element top) {
        try {
            return Integer.parseInt(getFirstElement(tagname, top));
        } catch (Exception e) {
            return -1;
        }
    }

    private String getAttribute(String tagname, String attribute, Element top) {
        Element el = getElement(tagname, top);

        if (el == null) {
            return "";
        } else {
            return el.getAttribute(attribute);
        }
    }
}