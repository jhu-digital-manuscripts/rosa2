package rosa.archive.core.serialize;

import com.google.inject.Inject;
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
    public void write(BookDescription object, OutputStream out) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
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
