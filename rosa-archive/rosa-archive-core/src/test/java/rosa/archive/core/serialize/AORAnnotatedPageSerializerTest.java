package rosa.archive.core.serialize;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.aor.AnnotatedPage;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
        } catch (IOException e) {
            System.out.println(errors);
        }

        assertNotNull(page);
        assertNull(page.getId());
        assertTrue(page.getSignature().equals(""));
        assertNotNull(page.getReader());
        assertNotNull(page.getPagination());
        assertEquals(52, page.getAnnotations().size());
    }

}
