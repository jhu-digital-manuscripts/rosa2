package rosa.archive.core.serialize;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.BookMetadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * @see rosa.archive.core.serialize.BookMetadataSerializer
 */
public class BookMetadataSerializerTest {

    private BookMetadataSerializer serializer;

    @Before
    public void setup() {
        this.serializer = new BookMetadataSerializer();
    }

    @Test
    public void readTest() throws IOException {
        final String testFile = "data/Walters143/Walters143.description_en.xml";

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(testFile)) {
            BookMetadata metadata = serializer.read(in);
            assertNotNull(metadata);

            assertEquals("14th century", metadata.getDate());
            assertEquals(1400, metadata.getYearStart());
            assertEquals(1300, metadata.getYearEnd());
            assertEquals("Baltimore, MD", metadata.getCurrentLocation());
            assertEquals("Walters Art Museum", metadata.getRepository());
            assertEquals("manuscript", metadata.getType());
            assertEquals(216, metadata.getWidth());
            assertEquals(300, metadata.getHeight());
            assertNotNull(metadata.getTexts());
            assertEquals(1, metadata.getTexts().length);
        }
    }

    @Test (expected = UnsupportedOperationException.class)
    public void writeTest() throws IOException {
        OutputStream out = mock(OutputStream.class);
        serializer.write(new BookMetadata(), out);
    }

}
