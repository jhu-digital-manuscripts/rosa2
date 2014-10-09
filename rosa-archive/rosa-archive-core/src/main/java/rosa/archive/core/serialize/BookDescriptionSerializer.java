package rosa.archive.core.serialize;

import com.google.inject.Inject;
import com.sun.org.apache.xml.internal.serializer.OutputPropertiesFactory;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import rosa.archive.core.config.AppConfig;
import rosa.archive.model.BookDescription;

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
import java.util.List;
import java.util.Map;

/**
 *
 */
public class BookDescriptionSerializer implements Serializer<BookDescription> {

    private AppConfig config;

    @Inject
    BookDescriptionSerializer(AppConfig config) {
        this.config = config;
    }

    @Override
    public BookDescription read(InputStream is, List<String> errors) throws IOException {

        try {

            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(is);
            return buildDescription(doc, errors);

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
    public void write(BookDescription description, OutputStream out) throws IOException {

        Document doc = newDocument();

        Element root = doc.createElement("TEI");
        root.setAttribute("xmlns", "http://www.tei-c.org/ns/1.0");
        root.setAttribute("version", "5.0");
        doc.appendChild(root);

        Element teiheader = doc.createElement("teiheader");
        root.appendChild(teiheader);

        Element fileDesc = doc.createElement("fileDesc");
        teiheader.appendChild(fileDesc);

        Element notesStmt = doc.createElement("notesStmt");
        fileDesc.appendChild(notesStmt);

        Map<String, Element> notes = description.getNotes();
        for (String key : notes.keySet()) { 
            notesStmt.appendChild(notes.get(key));
        }

        write(doc, out);

    }

    /**
     * @return a new DOM document
     */
    private Document newDocument() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            return null;
        }

        // TODO ask if it is fine to do it this way
        // These elements stored in the map belong to a different Document object!
        Document doc = builder.newDocument();
        doc.setStrictErrorChecking(false);
        return doc;
    }

    /**
     * @param doc document
     * @param out output stream
     */
    private void write(Document doc, OutputStream out) {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = null;
        try {
            transformer = transformerFactory.newTransformer();

            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            // Options to make it human readable
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputPropertiesFactory.S_KEY_INDENT_AMOUNT, "4");
        } catch (TransformerConfigurationException e) {
            return;
        }

        Source xmlSource = new DOMSource(doc);
        Result result = new StreamResult(out);

        try {
            transformer.transform(xmlSource, result);
        } catch (TransformerException e) {
            return;
        }
    }

    private BookDescription buildDescription(Document document, List<String> errors) {
        BookDescription description = new BookDescription();

        NodeList nodes = document.getElementsByTagName("notesStmt");
        if (nodes == null) {
            errors.add("<notesStmt> element is missing from description XML.");
            return description;
        } else if (nodes.getLength() != 1) {
            errors.add("Malformed XML document. Only one <nodesStmt> expected, instead there were [" + nodes.getLength() + "]");
            return description;
        }

        Map<String, Element> descriptionNotes = description.getNotes();

        NodeList notes = nodes.item(0).getChildNodes();
        for (int i = 0; i < notes.getLength(); i++) {
            Node note = notes.item(i);

            Element el;
            switch (note.getNodeType()) {
                case Node.ELEMENT_NODE:
                    el = (Element) note;

                    String rend = el.getAttribute("rend");
                    if (StringUtils.isBlank(rend)) {
                        break;
                    }

                    descriptionNotes.put(rend, el);

                    break;
                default:
                    break;
            }
        }

        return description;
    }
}
