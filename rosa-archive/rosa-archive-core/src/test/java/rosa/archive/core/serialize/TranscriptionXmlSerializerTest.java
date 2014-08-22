package rosa.archive.core.serialize;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import rosa.archive.core.ArchiveCoreModule;
import rosa.archive.core.GuiceJUnitRunner;
import rosa.archive.core.GuiceJUnitRunner.GuiceModules;
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
@RunWith(GuiceJUnitRunner.class)
@GuiceModules({ArchiveCoreModule.class})
public class TranscriptionXmlSerializerTest {

    @Inject
    private Serializer<Transcription> serializer;

    @Test
    public void readTest() throws IOException {
        final String testFile = "data/Walters143/Walters143.transcription.xml";

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(testFile)) {
            Transcription transcription = serializer.read(in);

            assertNotNull(transcription);
            assertNotNull(transcription.getContent());
            assertTrue(transcription.getContent().length() > 0);
        }

    }

    @Test (expected = UnsupportedOperationException.class)
    public void writeTest() throws IOException {
        OutputStream out = mock(OutputStream.class);
        serializer.write(new Transcription(), out);
    }

}
