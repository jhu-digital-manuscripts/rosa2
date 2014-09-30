package rosa.archive.core.serialize;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.SHA1Checksum;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * @see SHA1ChecksumSerializer
 */
public class SHA1ChecksumSerializerTest extends BaseSerializerTest {
    private static final String testFile = "data/Walters143/Walters143.SHA1SUM";

    private Serializer<SHA1Checksum> serializer;

    @Before
    public void setup() {
        super.setup();
        serializer = new SHA1ChecksumSerializer(config);
    }

    @Test
    public void readTest() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream(testFile);
        assertNotNull(is);

        SHA1Checksum info = serializer.read(is, errors);

        assertNotNull(info);
        assertEquals(13, info.getAllIds().size());

        String hash = info.checksums().get("Walters143.permission_en.html");
        assertEquals("9421c9c5988b83afb28eed96d60c5611b10d6336", hash);
    }

    @Test (expected = UnsupportedOperationException.class)
    public void writeTest() throws IOException {
        OutputStream out = mock(OutputStream.class);
        serializer.write(new SHA1Checksum(), out);
    }

}
