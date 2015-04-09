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

import static org.junit.Assert.assertFalse;

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
        System.out.println(prettyPrintXml(result));
    }

    @Test
    public void fsiShowcaseDocTest() throws IOException, XMLStreamException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        serializer.fsiShowcaseDoc(VALID_COLLECTION, loadValidLudwigXV7(), out);
        String result = out.toString();

        assertFalse("Results should not be empty.", result.isEmpty());
        System.out.println(prettyPrintXml(result));
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
