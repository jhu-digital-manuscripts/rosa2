package rosa.archive.core.serialize;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.BookDescription;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

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
        serializer = new BookDescriptionSerializer();
    }

    @Test
    public void readTest() throws IOException {
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
    public void writeTest() throws IOException {
        URL url = getClass().getClassLoader().getResource("data/LudwigXV7/LudwigXV7.description_en.xml");
        assertNotNull(url);

        BookDescription description = createDescription();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            serializer.write(description, out);
        }
    }

    private BookDescription createDescription() {
        BookDescription description = new BookDescription();

//        Map<String, Element> elMap = description.getNotes();


        return description;
    }

}
