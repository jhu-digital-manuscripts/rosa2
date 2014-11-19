package rosa.archive.core.serialize;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.core.config.AppConfig;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Errata;
import rosa.archive.model.aor.Mark;
import rosa.archive.model.aor.Symbol;
import rosa.archive.model.aor.Underline;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
            assertTrue(page.getErrata().size() == 0);
        }
    }

    @Test
    public void withErrataTest() throws IOException {
        final String file = "data/Ha2/Ha2.036r.xml";

        List<String> errors = new ArrayList<>();

        AnnotatedPage page = null;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(file)) {
            page = serializer.read(in, errors);
        }

        assertNotNull(page);
        assertTrue(errors.isEmpty());
        assertEquals(1, page.getErrata().size());
        assertEquals(36, page.getMarks().size());
        assertEquals(1, page.getSymbols().size());
        assertEquals(26, page.getUnderlines().size());
        assertEquals(8, page.getMarginalia().size());

        // errata
        Errata e = page.getErrata().get(0);
        assertNotNull(e);
        assertEquals("cacommodato", e.getCopyText());
        assertEquals("acommodato", e.getAmendedText());

        // <mark name="plus_sign" method="pen" place="intext" language="IT" text="Arguto"/>
        Mark m1 = new Mark("Arguto", "plus_sign", "pen", "intext", "IT");
        // <mark name="dash" method="pen" place="right_margin"/>
        Mark m2 = new Mark("", "dash", "pen", "right_margin", "");
        // fake mark
        Mark m3 = new Mark("fake text", "moo", "method", "space", "lang");
        assertTrue(page.getMarks().contains(m1));
        assertTrue(page.getMarks().contains(m2));
        assertFalse(page.getMarks().contains(m3));

        // <symbol name="Sun" place="left_margin"/>
        Symbol s1 = new Symbol("", "Sun", "left_margin");
        // not present in document
        Symbol s2 = new Symbol("fake text", "moo method", "in space place");
        assertTrue(page.getSymbols().contains(s1));
        assertFalse(page.getSymbols().contains(s2));

        // <underline method="pen" type="straight" language="IT" text="Arguto, &amp;"/>
        Underline u1 = new Underline("Arguto, &", "pen", "straight", "IT");
        // fake underline
        Underline u2 = new Underline("fake text", "moomethod", "asdf", "lang");
        assertTrue(page.getUnderlines().contains(u1));
        assertFalse(page.getUnderlines().contains(u2));
    }

    @Test
    public void readNonexistantFile() throws IOException {
        final String file = "fake file";
        List<String> errors = new ArrayList<>();

        AnnotatedPage page = serializer.read(null, errors);
        assertNull(page);
    }

    @Test (expected = UnsupportedOperationException.class)
    public void writeTest() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        serializer.write(new AnnotatedPage(), out);
    }

}
