package rosa.archive.core.serialize;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.ChecksumData;
import rosa.archive.model.ChecksumInfo;
import rosa.archive.model.HashAlgorithm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * @see rosa.archive.core.serialize.ChecksumInfoSerializer
 */
public class ChecksumInfoSerializerTest {
    private static final String testFile = "Walters143/Walters143.SHA1SUM";

    private ChecksumInfoSerializer serializer;

    @Before
    public void setup() {
        this.serializer = new ChecksumInfoSerializer();
    }

    @Test
    public void readTest() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream(testFile);
        assertNotNull(is);

        ChecksumInfo info = serializer.read(is);

        assertNotNull(info);
        assertEquals(13, info.getAllIds().size());

        ChecksumData data = info.getChecksumDataForId("Walters143.permission_en.html");
        assertEquals("9421c9c5988b83afb28eed96d60c5611b10d6336", data.getHash());
        assertEquals(HashAlgorithm.SHA1, data.getAlgorithm());
    }

    @Test (expected = UnsupportedOperationException.class)
    public void writeTest() throws IOException {
        OutputStream out = mock(OutputStream.class);
        serializer.write(new ChecksumInfo(), out);
    }

}
