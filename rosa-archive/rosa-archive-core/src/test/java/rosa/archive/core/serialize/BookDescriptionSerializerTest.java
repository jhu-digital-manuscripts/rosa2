package rosa.archive.core.serialize;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.BookDescription;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class BookDescriptionSerializerTest extends BaseSerializerTest<BookDescription> {

    String[] sections = {
            "IDENTIFICATION", "BASIC INFORMATION", "QUIRES", "MATERIAL", "LAYOUT", "SCRIPT", "DECORATION",
            "BINDING", "HISTORY", "TEXT"
    };

    @Before
    public void setup() {
        serializer = new BookDescriptionSerializer();
    }

    @Test
    public void readTest() throws IOException {
        BookDescription description = loadResource("data/Walters143/Walters143.description_en.xml");

        assertNotNull(description);
        assertEquals(10, description.getNotes().size());

        for (String section : sections) {
            assertTrue(description.getNotes().containsKey(section));
        }
    }

    @Test
    public void writeTest() throws IOException {
        writeObjectAndGetContent(createDescription());
    }

    /**
     * @return a BookDescription object to test the write method
     */
    private BookDescription createDescription() {
        BookDescription description = new BookDescription();

        return description;
    }

}
