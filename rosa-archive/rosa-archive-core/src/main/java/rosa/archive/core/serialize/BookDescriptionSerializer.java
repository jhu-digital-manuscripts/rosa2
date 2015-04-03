package rosa.archive.core.serialize;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import rosa.archive.core.util.XMLUtil;
import rosa.archive.model.BookDescription;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class BookDescriptionSerializer implements Serializer<BookDescription> {
    private static final String DESCRIPTION_START_TAG = "notesStmt";
    private static final String TOPIC_TAG = "note";
    private static final String REND_ATTR = "rend";

    private static final String P_TAG = "p";
    private static final String MATERIAL_TAG = "material";
    private static final String LIST_TAG = "list";
    private static final String ITEM_TAG = "item";
    private static final String HEAD_TAG = "head";
    private static final String LOCUS_TAG = "locus";
    private static final String HI_TAG = "hi";
    private static final String LINE_BREAK_TAG = "lb";

    @Override
    public BookDescription read(InputStream is, List<String> errors) throws IOException {

        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(is);

            return buildDescription(doc);
        } catch (ParserConfigurationException e) {
            errors.add("Error configuring XML parser.");
            throw new IOException(e);
        } catch (SAXException e) {
            errors.add("Error parsing XML.");
            throw new IOException(e);
        }
    }

    @Override
    public void write(BookDescription object, OutputStream out) throws IOException {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public Class<BookDescription> getObjectType() {
        return BookDescription.class;
    }

    private BookDescription buildDescription(Document doc) {
        BookDescription bookDescription = new BookDescription();

        NodeList list = doc.getElementsByTagName(DESCRIPTION_START_TAG);
        if (list == null || list.getLength() != 1) {
            return null;
        }

        Element start = (Element) list.item(0);
        NodeList notes = start.getElementsByTagName(TOPIC_TAG);
        for (int i = 0; i < notes.getLength(); i++) {
            Node n = notes.item(i);
            if (n.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element note = (Element) n;

            String topic = note.getAttribute(REND_ATTR);
            String description = elementToString(note);

            if (topic != null && description != null) {
                bookDescription.getBlocks().put(topic, description);
            }
        }

        return bookDescription;
    }

    private String elementToString(Element el) {
        Document doc = XMLUtil.newDocument();

        if (el == null || doc == null) {
            return null;
        }

        doc.appendChild(adaptToHtmlLike(el, doc));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XMLUtil.write(doc, out);

        return out.toString();
    }

    /*
        <p> - leave alone
        <material> - strip
        <list> - convert element + children to <ul>
            <head> - convert to <strong>?
            <item> - convert to <li>
                <locus> - grab text content only
        <hi rend="italics"> - convert to <span style="font-style: italic;"> preserving content
        <lb/> - convert to <br/>
     */
    private Node adaptToHtmlLike(Node node, Document doc) {

        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element el = (Element) node;
            String name = el.getTagName();


            switch (name) {
                case P_TAG:
                    Element p = doc.createElement(P_TAG);
                    doc.appendChild(p);

                    for (Node n = node.getFirstChild(); n != null; n = n.getNextSibling()) {
                        p.appendChild(adaptToHtmlLike(n, doc));
                    }
                    return p;
                case MATERIAL_TAG:
                    return doc.createTextNode(textContent(node));
                case LIST_TAG:
                    Element div = doc.createElement("div");
                    NodeList l = el.getElementsByTagName("head");
                    if (l != null && l.getLength() == 1) {
                        div.appendChild(adaptToHtmlLike(l.item(0), doc));
                    }

                    Element ul = doc.createElement("ul");
                    div.appendChild(ul);

                    NodeList list = el.getElementsByTagName(ITEM_TAG);
                    for (int i = 0; i < list.getLength(); i++) {
                        ul.appendChild(adaptToHtmlLike(list.item(i), doc));
                    }

                    return div;
                case HEAD_TAG:
                    Element span = doc.createElement("span");
                    span.setAttribute("style", "font-weight: bold;");
                    span.appendChild(doc.createTextNode(textContent(el)));
                    return span;
                case ITEM_TAG:
                    Element li = doc.createElement("li");

                    for (Node n = node.getFirstChild(); n != null; n = n.getNextSibling()) {
                        li.appendChild(adaptToHtmlLike(n, doc));
                    }

                    return li;
                case LOCUS_TAG:
                    String from = el.getAttribute("from");
                    String to = el.getAttribute("to");

                    return doc.createTextNode((from == null ? "" : from) + " - " + (to == null ? "" : to));
                case HI_TAG:
                    Element i = doc.createElement("span");
                    i.setAttribute("style", "font-style: italic;");

                    for (Node n = node.getFirstChild(); n != null; n = n.getNextSibling()) {
                        i.appendChild(adaptToHtmlLike(n, doc));
                    }
                    return i;
                case LINE_BREAK_TAG:
                    return doc.createElement("br");
                default:
                    break;
            }
        } else if (node.getNodeType() == Node.TEXT_NODE) {
            return doc.createTextNode(textContent(node));
        }

        return null;
    }

    private String textContent(Node node) {
        return node.getTextContent().replaceAll("\\s+", " ");
    }

}
