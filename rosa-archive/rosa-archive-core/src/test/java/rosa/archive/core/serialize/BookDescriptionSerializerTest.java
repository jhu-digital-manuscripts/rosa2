package rosa.archive.core.serialize;

import com.sun.org.apache.xerces.internal.dom.ElementImpl;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import rosa.archive.model.BookDescription;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class BookDescriptionSerializerTest extends BaseSerializerTest {

    private BookDescriptionSerializer serializer;

    String[] sections = {
            "IDENTIFICATION", "BASIC INFORMATION", "QUIRES", "MATERIAL", "LAYOUT", "SCRIPT", "DECORATION",
            "BINDING", "HISTORY", "TEXT"
    };

    @Before
    public void setup() {
        super.setup();
        serializer = new BookDescriptionSerializer(config);
    }

    @Test
    public void readTest() throws Exception {
        final String testFile = "data/Walters143/Walters143.description_en.xml";

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(testFile)) {
            BookDescription description = serializer.read(in, errors);

            assertNotNull(description);
            assertEquals(10, description.getNotes().size());

            for (String section : sections) {
                assertTrue(description.getNotes().containsKey(section));
            }
        }
    }

    @Test
    public void writeTest() throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
//        Document doc = builder.newDocument();
        Document doc = null;

        URL url = getClass().getClassLoader().getResource("data/LudwigXV7/LudwigXV7.description_en.xml");
        assertNotNull(url);

        BookDescription description = null;
        try (InputStream in = Files.newInputStream(Paths.get(url.toURI()))) {
//            doc = builder.parse(in);

            List<String> errors = new ArrayList<>();
            description = serializer.read(in, errors);
            System.out.println(description);
        }

        serializer.write(description, System.out);

//        BookDescription description = new BookDescription();
//        Map<String, Element> elementMap = description.getNotes();
//
//        description.setId("LudwigXV7.description_en.xml");
//

//        NodeList nodes = doc.getElementsByTagName("note");
//        for (int i = 0; i < nodes.getLength(); i++) {
//            Node note = nodes.item(i);
//
//            if (note.getNodeType() != Node.ELEMENT_NODE) {
//                continue;
//            }
//
//            Element el = (Element) note;
//            String rend = el.getAttribute("rend");
//
//            if (rend == null || rend.equals("")) {
//                continue;
//            }
//
//
//
//        }



    }

}
