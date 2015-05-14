package rosa.website.core.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.XMLParser;
import rosa.website.core.client.view.BookDescriptionView.Presenter;

public class BookDescriptionWidget extends Composite {
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

    private Presenter presenter;

    private final VerticalPanel root;

    public BookDescriptionWidget() {
        root = new VerticalPanel();
        initWidget(root);
    }

    public void setDescription(String xml) {
        if (isEmpty(xml)) {
            root.add(new Label("No description found."));
            return;
        }

        adaptToHtml(xml);
    }

    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    public void clear() {
        root.clear();
    }

    private void adaptToHtml(String xml) {
        Document doc = XMLParser.parse(xml);

        doc.normalize();
        NodeList list = doc.getElementsByTagName(DESCRIPTION_START_TAG);
        if (list == null || list.getLength() != 1) {
            return;
        }

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

            Label title = new Label(note.getAttribute(REND_ATTR));
            HTML description = new HTML(elementToString(note));

            root.add(title);
            root.add(description);
        }
    }

    private String elementToString(Element el) {
        Document doc = XMLParser.createDocument();

        if (el == null || doc == null) {
            return null;
        }

        Element root = doc.createElement("div");
        doc.appendChild(root);
        for (Node n = el.getFirstChild(); n != null; n = n.getNextSibling()) {
            root.appendChild(adaptToHtmlLike(n, doc));
        }

        return doc.toString();
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
     * <li>&lt;locus&gt; - links to the 'from' and 'to' pages. If pure number, assume recto</li>
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
                    return adaptToHtmlLike(node.getFirstChild(), doc);
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
                    span.appendChild(adaptToHtmlLike(node.getFirstChild(), doc));
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

                    Element locus = doc.createElement("span");

                    Element from_link = createPageLink(from, doc);
                    Element to_link = createPageLink(to, doc);

                    if (from_link != null) {
                        locus.appendChild(from_link);
                    }
                    locus.appendChild(doc.createTextNode(" - "));
                    if (to_link != null) {
                        locus.appendChild(to_link);
                    }

                    return locus;
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
            return doc.createTextNode(node.getNodeValue());
        }

        return null;
    }

    // TODO Note: methods below ripped/adapted from BookMetadataWidget!!
    /**
     * @param page .
     * @return link to read specified page, or a label if no such link exists
     */
    private Element createPageLink(String page, Document doc) {
        if (isNotEmpty(page)) {
            Element anchor = doc.createElement("a");

            anchor.appendChild(doc.createTextNode(page));
            if (isNumeric(page)) {
                anchor.setAttribute("href",
                        GWT.getHostPageBaseURL() + "#" + presenter.getPageUrlFragment(parseInt(page)));
            } else if (isRectoVerso(page)) {
                anchor.setAttribute("href", GWT.getHostPageBaseURL() + "#" + presenter.getPageUrlFragment(page));
            }

            return anchor;
        }

        return null;
    }

    private boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    private boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    private boolean isRectoVerso(String page) {
        return page.endsWith("r") || page.endsWith("v") || page.endsWith("R") || page.endsWith("V");
    }

    /**
     * @param str .
     * @return is this string a number
     */
    private native boolean isNumeric(String str) /*-{
        return !isNaN(str);
    }-*/;

    private native int parseInt(String str) /*-{
        return parseInt(str);
    }-*/;
}
