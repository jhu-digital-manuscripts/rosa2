package rosa.archive.core.serialize;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.core.config.AppConfig;
import rosa.archive.model.aor.AnnotatedPage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class AORAnnotatedPageSerializerTest extends BaseSerializerTest {

    private AORAnnotatedPageSerializer serializer;

    @Before
    public void setup() {
        AppConfig config = mock(AppConfig.class);
        serializer = new AORAnnotatedPageSerializer(config);

        when(config.getAnnotationSchemaUrl()).thenReturn("http://www.livesandletters.ac.uk/schema/aor_20141023.xsd");
        when(config.getAnnotationDtdUrl()).thenReturn("http://www.livesandletters.ac.uk/schema/aor_20141023.dtd");
    }

    @Test
    public void readTest() throws IOException {
        final String[] files = {
                "data/Ha2/Ha2.018r.xml", "data/Ha2/Ha2.018v.xml",
                "data/Ha2/Ha2.019r.xml", "data/Ha2/Ha2.019v.xml",
                "data/Ha2/Ha2.020r.xml", "data/Ha2/Ha2.020v.xml",
                "data/Ha2/Ha2.021r.xml", "data/Ha2/Ha2.021v.xml"
        };
        List<String> errors = new ArrayList<>();

        AnnotatedPage page = null;
        for (String testFile : files) {
            errors.clear();

            try (InputStream in = getClass().getClassLoader().getResourceAsStream(testFile)) {
                page = serializer.read(in, errors);
            }

            assertEquals(0, errors.size());
            assertNotNull(page);
            assertNull(page.getId());
            assertTrue(page.getSignature().equals(""));
            assertNotNull(page.getReader());
            assertNotNull(page.getPagination());
            assertTrue(page.getMarginalia().size() > 0);
            assertTrue(page.getUnderlines().size() > 0);
        }
    }

    @Test (expected = UnsupportedOperationException.class)
    public void writeTest() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        serializer.write(new AnnotatedPage(), out);
    }

}
