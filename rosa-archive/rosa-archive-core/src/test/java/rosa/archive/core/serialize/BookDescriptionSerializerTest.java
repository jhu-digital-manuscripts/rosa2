package rosa.archive.core.serialize;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.BookDescription;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class BookDescriptionSerializerTest extends BaseSerializerTest<BookDescription> {

    @Before
    public void setup() {
        this.serializer = new BookDescriptionSerializer();
    }

    @Override
    public void readTest() throws IOException {
        final String testFile = "LudwigXV7.description_en.xml";

        BookDescription description = loadResource(COLLECTION_NAME, BOOK_NAME, testFile);
        assertNotNull("Failed to load description.", description);

        assertNotNull(description.getXML());
//        List<String> topics = description.getTopics();
//        assertNotNull("Failed to get list of topics.", topics);
//        assertEquals("Unexpected number of topics found.", 11, topics.size());
//
//        for (String topic : topics) {
//            String desc = description.asString(topic);
//            assertNotNull(desc);
//            assertFalse(desc.isEmpty());
//        }
    }

    @Override
    @Test(expected = UnsupportedOperationException.class)
    public void writeTest() throws IOException {
        serializer.write(null, null);
    }
}
