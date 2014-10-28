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
//        final String testFile = "data/Ha2/Ha2.019r.xml";
        final String testFile = "data/Ha2/Ha2.019v.xml";
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
        assertEquals(6, page.getMarginalia().size());
        assertEquals(12, page.getMarks().size());
        assertEquals(3, page.getSymbols().size());
        assertEquals(29, page.getUnderlines().size());
        assertEquals(0, page.getNumerals().size());
    }

}
