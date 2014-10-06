package rosa.archive.core.serialize;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.BookMetadata;
import sun.nio.ch.IOUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @see rosa.archive.core.serialize.BookMetadataSerializer
 */
public class BookMetadataSerializerTest extends BaseSerializerTest {

    private Serializer<BookMetadata> serializer;

    @Before
    public void setup() {
        super.setup();
        serializer = new BookMetadataSerializer(config);
    }

    @Test
    public void readTest() throws IOException {
        final String testFile = "data/Walters143/Walters143.description_en.xml";

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(testFile)) {
            BookMetadata metadata = serializer.read(in, errors);
            assertNotNull(metadata);

            assertEquals("14th century", metadata.getDate());
            assertEquals(1400, metadata.getYearStart());
            assertEquals(1300, metadata.getYearEnd());
            assertEquals("Baltimore, MD", metadata.getCurrentLocation());
            assertEquals("Walters Art Museum", metadata.getRepository());
            assertNotNull(metadata.getShelfmark());
            assertNotNull(metadata.getOrigin());
            assertEquals("manuscript", metadata.getType());
            assertNotNull(metadata.getDimensions());
            assertEquals(216, metadata.getWidth());
            assertEquals(300, metadata.getHeight());
            assertTrue(metadata.getNumberOfIllustrations() > -1);
            assertTrue(metadata.getNumberOfPages() > -1);
            assertNotNull(metadata.getCommonName());
            assertNotNull(metadata.getMaterial());

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
