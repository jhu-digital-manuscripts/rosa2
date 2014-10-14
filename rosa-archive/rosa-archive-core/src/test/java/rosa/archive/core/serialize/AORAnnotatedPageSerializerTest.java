package rosa.archive.core.serialize;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.aor.AnnotatedPage;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;

/**
 *
 */
public class AORAnnotatedPageSerializerTest extends BaseSerializerTest {

    private AORAnnotatedPageSerializer serializer;

    @Before
    public void setup() {
        serializer = new AORAnnotatedPageSerializer();
    }

    @Test
    public void readTest() throws IOException {
//        final String testFile = "data/Domenichi/Domenichi_007right.xml";
        final String testFile = "data/Domenichi/Domenichi_007left.xml";
        List<String> errors = new ArrayList<>();

        AnnotatedPage page = null;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(testFile)) {
            page = serializer.read(in, errors);
        }
        System.out.println(page);
        assertNotNull(page);
    }

}
