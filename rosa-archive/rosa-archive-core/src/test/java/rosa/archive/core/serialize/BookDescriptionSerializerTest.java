package rosa.archive.core.serialize;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import rosa.archive.model.BookDescription;

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

    @Ignore
    @Override
    public void roundTripTest() throws IOException {

    }

    @Override
    protected BookDescription createObject() {
        return null;
    }
}
