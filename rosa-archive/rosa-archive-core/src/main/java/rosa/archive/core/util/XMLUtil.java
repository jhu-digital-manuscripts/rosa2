package rosa.archive.core.util;

import com.sun.org.apache.xml.internal.serializer.OutputPropertiesFactory;
import org.w3c.dom.Document;
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
 *
 */
public class XMLUtil {
    private static final int MAX_CACHE_SIZE = 100;
    private static final ConcurrentHashMap<String, Schema> schemaCache = new ConcurrentHashMap<>();

    /**
     * @return a new DOM document
     */
    public static Document newDocument() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            return null;
        }

        return builder.newDocument();
    }

    // TODO crappy place for a cache....
    public static Document newDocument(String schemaUrl) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        Schema schema = null;
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

        DocumentBuilder builder = null;
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
     */
    public static void write(Document doc, OutputStream out) {
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

}
