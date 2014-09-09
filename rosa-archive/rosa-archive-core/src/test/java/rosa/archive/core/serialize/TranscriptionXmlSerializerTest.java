package rosa.archive.core.serialize;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.Transcription;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @see TranscriptionXmlSerializerTest
 */
public class TranscriptionXmlSerializerTest extends BaseSerializerTest {

    private Serializer<Transcription> serializer;

    @Before
    public void setup() {
        super.setup();
        serializer = new TranscriptionXmlSerializer(config);
    }

    @Test
    public void readTest() throws IOException {
        final String testFile = "data/Walters143/Walters143.transcription.xml";

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(testFile)) {
            Transcription transcription = serializer.read(in, errors);

            assertNotNull(transcription);
            assertNotNull(transcription.getContent());
            assertTrue(transcription.getContent().length() > 0);
            assertTrue(errors.isEmpty());
        }

    }

    @Test (expected = UnsupportedOperationException.class)
    public void writeTest() throws IOException {
        OutputStream out = mock(OutputStream.class);
        serializer.write(new Transcription(), out);
    }

}
