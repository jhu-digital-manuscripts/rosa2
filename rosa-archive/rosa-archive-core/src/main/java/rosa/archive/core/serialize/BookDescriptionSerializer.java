package rosa.archive.core.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import rosa.archive.core.config.AppConfig;
import rosa.archive.core.util.XMLUtil;
import rosa.archive.model.BookDescription;

import com.google.inject.Inject;

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
        Document doc = XMLUtil.newDocument();

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
            notesStmt.appendChild(doc.importNode(notes.get(key), true));
        }

        XMLUtil.write(doc, out);
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
