package rosa.archive.core.serialize;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.CropData;
import rosa.archive.model.CropInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * @see rosa.archive.core.serialize.CropInfoSerializer
 */
public class CropInfoSerializerTest extends BaseSerializerTest {

    private Serializer<CropInfo> serializer;

    @Before
    public void setup() {
        super.setup();
        serializer = new CropInfoSerializer(config);
    }

    @Test
    public void readTest() throws IOException {
        final String testFile = "data/Walters143/Walters143.crop.txt";

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(testFile)) {
            CropInfo info = serializer.read(in, errors);
            assertNotNull(info);

            CropData data = info.getCropDataForPage("Walters143.039v.tif");
            double delta = 0.0000001;
            assertEquals("Walters143.039v.tif", data.getId());
            assertEquals(0.119649, data.getLeft(), delta);
            assertEquals(0.036224, data.getRight(), delta);
            assertEquals(0.036667, data.getTop(), delta);
            assertEquals(0.016667, data.getBottom(), delta);
        }
    }

    @Test (expected = UnsupportedOperationException.class)
    public void writeTest() throws IOException {
        OutputStream out = mock(OutputStream.class);
        serializer.write(new CropInfo(), out);
    }

}
