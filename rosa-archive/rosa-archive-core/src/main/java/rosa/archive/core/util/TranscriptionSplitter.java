package rosa.archive.core.util;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import rosa.archive.model.Transcription;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class that takes transcription XML and splits it by page.
 */
public class TranscriptionSplitter {
    private static final Logger log = Logger.getLogger(TranscriptionSplitter.class.toString());
    private static final Pattern pagePattern = Pattern.compile("(\\d+)(r|v)");

    private static DocumentBuilder docBuilder;

    /**
     * Split up transcription data for a book into fragments according to page and
     * column.
     *
     * {@see #split(String)}
     *
     * @param transcription transcription object from a book
     * @return transcription text split per page
     */
    public static Map<String, String> split(Transcription transcription) {
        if (transcription == null
                || transcription.getXML() == null || transcription.getXML().isEmpty()) {
            return Collections.emptyMap();
        }

        return split(transcription.getXML());
    }

    /**
     * Split up the transcription XML into fragments according to page and column. All
     * XML fragments are kept as Strings.
     *
     * The map produced by this method will contain entries that relate page number/folio
     * [short name?](ex: 135r) to the XML fragment representing the transcription on
     * that page. The XML fragment SHOULD start with a &lt;cb&gt; tag, indicating the
     * first column on that page. Any other columns will also be marked with the same
     * tag.
     *
     * Note that the transcription may be recorded in couplets and new columns
     * might split couplets. This results in a &lt;cb&gt; tag marking a new column
     * inside a &lt;lg&gt; couplet tag. Anyone parsing these XML fragments will have
     * to take this into consideration.
     *
     * @param xml original transcription XML containing all transcriptions
     * @return map of page TO transcription XML fragment
     */
    public static Map<String, String> split(String xml) {
        Document doc = readXml(xml);
        if (doc == null) {
            return null;
        }
        doc.normalizeDocument();

        // Find the index of each top level <pb> tag
        List<Integer> pbs = new ArrayList<>();

        NodeList bodyList = doc.getDocumentElement().getElementsByTagName("body");
        if (bodyList == null || bodyList.getLength() != 1) {
            log.warning("Cannot parse transcription data. Invalid XML structure. " +
                    "(There should be exactly one <body> tag)");
            return new HashMap<>();
        }

        NodeList all_nodes = bodyList.item(0).getChildNodes();
        if (all_nodes.getLength() == 1) {
            all_nodes = all_nodes.item(0).getChildNodes();
        } else if (all_nodes.getLength() > 1){
            for (int i = 0; i < all_nodes.getLength(); i++) {
                Node n = all_nodes.item(i);

                if (n.getNodeName().equalsIgnoreCase("div")) {
                    all_nodes = all_nodes.item(i).getChildNodes();
                    break;
                }
            }

        }

        // TODO need to account for <pb> tags inside of <lg> tags

        for (int i = 0; i < all_nodes.getLength(); i++) {
            Node n = all_nodes.item(i);

            if (n.getNodeName().equals("pb")) {
                pbs.add(i);
            }
        }

        return slitByPage(pbs.toArray(new Integer[pbs.size()]), all_nodes);
    }

    /**
     * Split the original XML document up according to <pb> tags
     *
     * NOTE: There is some odd cases in the transcription.xml where the designation
     * for a new column on a page appears inside of an <lg> tag, if a couplet
     * happens to fall across two columns. This case is not handled here.
     *
     * @param pbs list of indexes (line number) where &lt;pb&gt; tags can be found.
     * @param all_nodes all child nodes of the base &lt;div&gt; tag inside the body.
     * @return a map of pages -&gt; xml fragments
     */
    private static Map<String, String> slitByPage(Integer[] pbs, NodeList all_nodes) {
        Map<String, String> map = new HashMap<>();

        for (int index = 0; index < pbs.length - 1; index++) {
            int start = pbs[index];
            int end = pbs[index + 1];

            Document fragment = XMLUtil.newDocument();
            fragment.appendChild(fragment.createElement("div"));

            Node pb_start = all_nodes.item(start);
            String page = findPage(getAttribute("n", pb_start));

            if (page == null) {
                continue;
            }

            for (int i = start + 1; i < end; i++) {
                fragment.getDocumentElement().appendChild(fragment.importNode(all_nodes.item(i), true));
            }

            addToMap(page, fragment, map);
        }

        return map;
    }

    private static void addToMap(String page, Document toAdd, Map<String, String> pageMap) {
        if (toAdd == null || pageMap == null) {
            return;
        }

        // Simply add the document to map if the page is not present
        if (!pageMap.containsKey(page)) {
            pageMap.put(page, toString(toAdd));
            return;
        }

        // Page already exists in the map, must smush 2 XML fragments together
        Document original = readXml(pageMap.get(page));
        if (original == null) {
            // Failed to parse first page fragment
            pageMap.put(page, toString(toAdd));
            return;
        }

        original.adoptNode(toAdd.getDocumentElement());
        pageMap.put(page, toString(original));
    }

    /**
     * @param filename page name
     * @return standard form
     */
    private static String findPage(String filename) {
        Matcher m = pagePattern.matcher(filename);

        if (m.find()) {
            int n = Integer.parseInt(m.group(1));
            return String.format("%03d", n) + m.group(2);
        } else {
            log.warning("Failed to find page while splitting transcriptions. [" + filename + "]");
            return null;
        }
    }

    private static String getAttribute(String attribute, Node node) {
        return node.getAttributes().getNamedItem(attribute).getNodeValue();
    }

    /**
     * Parse a String containing XML.
     *
     * @param xml XML as a String
     * @return XML Document
     */
    private static Document readXml(String xml) {
        try {
            if (docBuilder == null) {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(true);
                docBuilder = dbf.newDocumentBuilder();
            }
            return docBuilder.parse(new InputSource(new StringReader(xml)));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            log.log(Level.SEVERE, "Failed to parse XML.", e);
        }
        return null;
    }

    /**
     * Print an XML Document to a String.
     *
     * @param doc document to print
     * @return String form of document
     */
    private static String toString(Document doc) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XMLUtil.write(doc, out, true);
        return out.toString();
    }

}
