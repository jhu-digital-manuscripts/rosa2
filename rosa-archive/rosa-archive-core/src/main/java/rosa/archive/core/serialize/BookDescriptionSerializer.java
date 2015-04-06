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
import javax.xml.transform.OutputKeys;
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

    private Element listTable;

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

        listTable = doc.createElement("table");

        /*
            For each <note> tag, create a new topic. Subject is set to the 'rend'
            attribute of the <note> tag. Content is set to an HTML adaptation of
            the TEI contained within the <note>.
        */
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

        Element root = doc.createElement("div");
        doc.appendChild(root);
        for (Node n = el.getFirstChild(); n != null; n = n.getNextSibling()) {
            root.appendChild(adaptToHtmlLike(n, doc));
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XMLUtil.write(doc, out, true);

        return out.toString();
    }

    /**
     * Adapt the transcription TEI XML into HTML that can be presented by
     * a web browser. This is done by simple tag substitutions:
     *
     * <ul>
     * <li>&lt;p&gt; - leave alone</li>
     * <li>&lt;material&gt; - strip (take only content of this node)</li>
     * <li>&lt;list&gt; - convert element and content to &lt;ul&gt; and children</li>
     * <li>&lt;head&gt; - convert to span?</li>
     * <li>&lt;item&gt; - convert to &lt;li&gt;</li>
     * <li>&lt;locus&gt; - grab text content</li>
     * <li>&lt;hi rend="italics"&gt; - convert to &lt;span style="font-style: italic;"&gt;
     *     while preserving content</li>
     * <li>&lt;lb/&gt; - convert to &lt;br/&gt;</li>
     * </ul>
     *
     * For source, check the Rosa 1 TranscriptionViewer, which adapts the TEI to GWT HTML
     * elements.
     *
     * https://github.com/jhu-digital-manuscripts/rosa/blob/master/rosa-website-common/src/main/java/rosa/gwt/common/client/TranscriptionViewer.java
     *
     * @param node node to adapt
     * @param doc xml document
     * @return adapted node
     */
    private Node adaptToHtmlLike(Node node, Document doc) {

        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element el = (Element) node;
            String name = el.getTagName();

            switch (name) {
                case P_TAG:
                    Element p = doc.createElement(P_TAG);

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

                    Element table = doc.createElement("table");
                    Element tbody = doc.createElement("tbody");
                    div.appendChild(table);
                    table.appendChild(tbody);

                    NodeList list = el.getElementsByTagName(ITEM_TAG);
                    for (int i = 0; i < list.getLength(); i++) {
                        tbody.appendChild(adaptToHtmlLike(list.item(i), doc));
                    }

                    return div;
                case HEAD_TAG:
                    Element span = doc.createElement("span");

                    span.setAttribute("style", "font-weight: bold;");
                    span.appendChild(doc.createTextNode(textContent(el)));
                    return span;
                case ITEM_TAG:
                    String item = el.getAttribute("n");
                    Element tr = doc.createElement("tr");

                    if (item != null && !item.isEmpty()) {
                        Element td = doc.createElement("td");
                        tr.appendChild(td);
                        td.appendChild(doc.createTextNode(item + " "));
                    }
                    Element td = doc.createElement("td");
                    tr.appendChild(td);
                    for (Node n = node.getFirstChild(); n != null; n = n.getNextSibling()) {
                        td.appendChild(adaptToHtmlLike(n, doc));
                    }

                    return tr;
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
