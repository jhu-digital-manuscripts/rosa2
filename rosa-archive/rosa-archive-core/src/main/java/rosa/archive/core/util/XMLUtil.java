package rosa.archive.core.util;

import com.sun.org.apache.xml.internal.serializer.OutputPropertiesFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
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
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO centralize more of the XML code? Instead of having crap done in a "Util" class...
 */
public class XMLUtil {
    private static final int MAX_CACHE_SIZE = 100;
    private static final ConcurrentHashMap<String, Schema> schemaCache = new ConcurrentHashMap<>();

    /**
     * @return a new DOM document
     */
    public static Document newDocument() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }

        return builder.newDocument();
    }

    /**
     * @param schemaUrl URL of schema to attach
     * @return a document builder factory with a schema set
     */
    public static DocumentBuilderFactory documentBuilderFactory(String schemaUrl) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        Schema schema;
        if (schemaCache.containsKey(schemaUrl)) {
            schema = schemaCache.get(schemaUrl);
        } else {
            try {
                URL url = new URL(schemaUrl);

                SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                schema = factory.newSchema(url);

                if (schemaCache.size() > MAX_CACHE_SIZE) {
                    schemaCache.clear();
                }
                schemaCache.putIfAbsent(schemaUrl, schema);

            } catch (MalformedURLException | SAXException e) {
                return null;
            }
        }

        if (schema != null) {
            dbf.setSchema(schema);
        }

        return dbf;
    }

    /**
     * @param schemaUrl URL of schema to attach to the document
     * @return a new empty Document
     */
    public static Document newDocument(String schemaUrl) {
        DocumentBuilderFactory dbf = documentBuilderFactory(schemaUrl);
        if (dbf == null) {
            return null;
        }

        DocumentBuilder builder;
        try {
            builder = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            return null;
        }

        return builder.newDocument();
    }

    /**
     * @param doc document
     * @param out output stream
     * @param omitXmlDeclaration .
     */
    public static void write(Document doc, OutputStream out, boolean omitXmlDeclaration) {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = transformerFactory.newTransformer();

            if (omitXmlDeclaration) {
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            } else {
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            }
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

    public static void removeChildren(Node parent) {
        for (;;) {
            Node n = parent.getFirstChild();

            if (n == null) {
                break;
            }

            parent.removeChild(n);
        }
    }

    public static void removeChildren(Document doc) {
        if (doc.getDocumentElement() != null ) {
            removeChildren(doc.getDocumentElement());
        }
    }

}
