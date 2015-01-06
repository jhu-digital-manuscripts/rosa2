package rosa.archive.core.serialize;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.Transcription;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @see TranscriptionXmlSerializerTest
 */
public class TranscriptionXmlSerializerTest extends BaseSerializerTest<Transcription> {

    @Before
    public void setup() {
        serializer = new TranscriptionXmlSerializer();
    }

    @Test
    public void readTest() throws IOException {
        List<String> errors = new ArrayList<>();
        Transcription transcription = loadResource(COLLECTION_NAME, BOOK_NAME, "LudwigXV7.transcription.xml", errors);

        assertNotNull(transcription);
        assertNotNull(transcription.getContent());
        assertTrue(transcription.getContent().length() > 0);
        assertTrue(errors.isEmpty());
    }

    @Test (expected = UnsupportedOperationException.class)
    public void writeTest() throws IOException {
        writeObjectAndGetContent(new Transcription());
    }

}
