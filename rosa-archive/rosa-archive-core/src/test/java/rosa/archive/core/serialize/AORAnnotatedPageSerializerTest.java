package rosa.archive.core.serialize;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Errata;
import rosa.archive.model.aor.Location;
import rosa.archive.model.aor.Mark;
import rosa.archive.model.aor.Symbol;
import rosa.archive.model.aor.Underline;

/**
 * TODO after importing update AoR data
 */
public class AORAnnotatedPageSerializerTest extends BaseSerializerTest<AnnotatedPage> {
    private static final String BOOK_NAME = "FolgersHa2";

    @Before
    public void setup() {
        serializer = new AORAnnotatedPageSerializer();
    }

    @Test
    public void readTest() throws IOException {
        final String[] files = {
                "FolgersHa2.018r.xml", "FolgersHa2.018v.xml",
                "FolgersHa2.019r.xml", "FolgersHa2.019v.xml",
                "FolgersHa2.020r.xml", "FolgersHa2.020v.xml",
                "FolgersHa2.021r.xml", "FolgersHa2.021v.xml"
        };
        List<String> errors = new ArrayList<>();

        AnnotatedPage page = null;
        for (String testFile : files) {
            errors.clear();
            page = loadResource(COLLECTION_NAME, BOOK_NAME, testFile, errors);

            assertTrue(errors.isEmpty());
            assertNotNull(page);
            assertEquals(testFile, page.getId());
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
        List<String> errors = new ArrayList<>();
        AnnotatedPage page = loadResource(COLLECTION_NAME, BOOK_NAME, "FolgersHa2.036r.xml", errors);

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
        Mark m1 = new Mark("Arguto", "plus_sign", "pen", "IT", Location.INTEXT);
        // <mark name="dash" method="pen" place="right_margin"/>
        Mark m2 = new Mark("", "dash", "pen", "", Location.RIGHT_MARGIN);
        // fake mark
        Mark m3 = new Mark("fake text", "moo", "method", "lang", null);
        assertTrue(page.getMarks().contains(m1));
        assertTrue(page.getMarks().contains(m2));
        assertFalse(page.getMarks().contains(m3));

        // <symbol name="Sun" place="left_margin"/>
        Symbol s1 = new Symbol("", "Sun", Location.LEFT_MARGIN);
        // not present in document
        Symbol s2 = new Symbol("fake text", "moo method", null);
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
        AnnotatedPage page = loadResource(COLLECTION_NAME, BOOK_NAME, "FolgersHa2.00v.xml");
        assertNull(page);
    }

    @Test (expected = UnsupportedOperationException.class)
    public void writeTest() throws IOException {
        writeObjectAndGetContent(new AnnotatedPage());
    }

}
