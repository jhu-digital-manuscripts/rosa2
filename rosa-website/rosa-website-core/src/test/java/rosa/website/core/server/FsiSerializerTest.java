package rosa.website.core.server;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import rosa.archive.core.BaseArchiveTest;
import rosa.archive.core.util.XMLUtil;
import rosa.archive.model.Book;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class FsiSerializerTest extends BaseArchiveTest {
    private FsiSerializer serializer;

    @Before
    public void setup() {
        Map<String, String> fsi_share_map = new HashMap<>();
        fsi_share_map.put("valid", "valid");

        serializer = new FsiSerializer(fsi_share_map);
    }

    @Test
    public void fsiPagesDocTest() throws IOException, XMLStreamException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Book book = loadValidLudwigXV7();

        serializer.fsiPagesDoc(VALID_COLLECTION, book, out);
        String result = out.toString();

        assertFalse("Results should not be empty.", result.isEmpty());

        // Pages does not include the back cover
        Document doc = parseDocString(result);
        assertNotNull("XML Document not found.", doc);
        assertEquals("Unexpected number of FSI plugins found.", 4, numberOfTags("PLUGIN", doc));
        assertEquals("Unexpected number of images found.", 286, numberOfTags("Image", doc));
        assertEquals("Unexpected number of FPX tags found.", 286, numberOfTags("FPX", doc));
        assertEquals("Unexpected number of image source tags found.", 286, numberOfTags("Src", doc));
    }

    @Test
    public void fsiShowcaseDocTest() throws IOException, XMLStreamException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        serializer.fsiShowcaseDoc(VALID_COLLECTION, loadValidLudwigXV7(), out);
        String result = out.toString();

        assertFalse("Results should not be empty.", result.isEmpty());

        // Showcase includes back cover
        Document doc = parseDocString(result);
        assertNotNull("XML Document not found.", doc);
        assertEquals("Unexpected number of FSI plugins found.", 4, numberOfTags("PLUGIN", doc));
        assertEquals("Unexpected number of images found.", 287, numberOfTags("Image", doc));
        assertEquals("Unexpected number of FPX tags found.", 287, numberOfTags("FPX", doc));
        assertEquals("Unexpected number of image source tags found.", 287, numberOfTags("Src", doc));
    }

    private int numberOfTags(String name, Document doc) {
        return doc.getElementsByTagName(name).getLength();
    }

    private Document parseDocString(String xml) {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();

        try {
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(xml)));

        } catch (ParserConfigurationException | SAXException | IOException e) {
            fail("Failed to parse XML string. \n[" + prettyPrintXml(xml) + "]");
            return null;
        }
    }

    private String prettyPrintXml(String xml) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));

            XMLUtil.write(doc, out, false);
        } catch (ParserConfigurationException | IOException | SAXException e) {
            System.err.println("Failed to parse XML string.");
        }

        return out.toString();
    }

}
