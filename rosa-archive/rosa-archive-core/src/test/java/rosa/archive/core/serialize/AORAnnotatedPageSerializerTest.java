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
import org.junit.Test;

import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Errata;
import rosa.archive.model.aor.Location;
import rosa.archive.model.aor.Mark;
import rosa.archive.model.aor.Symbol;
import rosa.archive.model.aor.Underline;

/**
 *
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
                "FolgersHa2.aor.018r.xml", "FolgersHa2.aor.018v.xml",
                "FolgersHa2.aor.019r.xml", "FolgersHa2.aor.019v.xml",
                "FolgersHa2.aor.020r.xml", "FolgersHa2.aor.020v.xml",
                "FolgersHa2.aor.021r.xml", "FolgersHa2.aor.021v.xml"
        };
        List<String> errors = new ArrayList<>();

        for (String testFile : files) {
            errors.clear();
            AnnotatedPage page = loadResource(COLLECTION_NAME, BOOK_NAME, testFile, errors);

            assertTrue("Errors list should be empty.", errors.isEmpty());
            assertNotNull("Failed to load AoR transcription.", page);
            assertEquals("Loaded wrong page.", testFile, page.getId());
            assertTrue("Page signature should be empty.", page.getSignature().equals(""));
            assertNotNull("Page reader should not be null.", page.getReader());
            assertNotNull("Pagination should not be null.", page.getPagination());
            assertFalse("Marginalia list should not be empty.", page.getMarginalia().isEmpty());
            assertFalse("Underlines list should not be empty.", page.getUnderlines().isEmpty());
            assertTrue("Errata list should be empty.", page.getErrata().isEmpty());
        }
    }

    @Test
    public void withErrataTest() throws IOException {
        List<String> errors = new ArrayList<>();
        AnnotatedPage page = loadResource(COLLECTION_NAME, BOOK_NAME, "FolgersHa2.aor.036r.xml", errors);

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
        Mark m1 = new Mark("FolgersHa2.036r.tif_36", "Arguto", "plus_sign", "pen", "IT", Location.INTEXT);
        // <mark name="dash" method="pen" place="right_margin"/>
        Mark m2 = new Mark("FolgersHa2.036r.tif_2", "", "dash", "pen", "", Location.RIGHT_MARGIN);
        // fake mark
        Mark m3 = new Mark("id", "fake text", "moo", "method", "lang", null);

        assertTrue(page.getMarks().contains(m1));
        assertTrue(page.getMarks().contains(m2));
        assertFalse(page.getMarks().contains(m3));

        // <symbol name="Sun" place="left_margin"/>
        Symbol s1 = new Symbol("FolgersHa2.036r.tif_1", "", "Sun", "", Location.LEFT_MARGIN);
        // not present in document
        Symbol s2 = new Symbol("id", "fake text", "moo method", "lang", null);
        assertTrue(page.getSymbols().contains(s1));
        assertFalse(page.getSymbols().contains(s2));

        // <underline method="pen" type="straight" language="IT" text="Arguto, &amp;"/>
        Underline u1 = new Underline("FolgersHa2.036r.tif_26", "Arguto, &", "pen", "straight", "IT");
        // fake underline
        Underline u2 = new Underline("id", "fake text", "moomethod", "asdf", "lang");
        assertTrue(page.getUnderlines().contains(u1));
        assertFalse(page.getUnderlines().contains(u2));
    }

    @Test
    public void readNonexistantFile() throws IOException {
        AnnotatedPage page = loadResource(COLLECTION_NAME, BOOK_NAME, "FolgersHa2.aor.00v.xml");
        assertNull(page);
    }

    @Test
    public void writeTest() throws IOException {
        AnnotatedPage page1 = loadResource(COLLECTION_NAME, BOOK_NAME, "FolgersHa2.aor.024r.xml");
        assertNotNull(page1);

        page1.setPage("FolgersHa2.000v.tif");

        List<String> lines = writeObjectAndGetWrittenLines(page1);

        assertNotNull(lines);
        assertEquals(194, lines.size());
        assertTrue(lines.get(1).startsWith("<transcription"));
        assertTrue(lines.get(2).contains("filename=\"FolgersHa2.000v.tif\""));
        assertFalse(lines.get(2).contains("filename=\"FolgersHa2.024r.tif\""));

        int marginalia = 0, underlines = 0, symbols = 0, marks = 0, errata = 0, numerals = 0;
        for (String line : lines) {
            if (line.contains("<marginalia")) {
                marginalia++;
            } else if (line.contains("<underline")) {
                underlines++;
            } else if (line.contains("<symbol")) {
                symbols++;
            } else if (line.contains("<mark")) {
                marks++;
            } else if (line.contains("<errata")) {
                errata++;
            } else if (line.contains("<numeral")) {
                numerals++;
            }
        }

        assertEquals(19, marginalia);
        assertEquals(34, underlines);
        assertEquals(3, symbols);
        assertEquals(32, marks);
        assertEquals(0, errata);
        assertEquals(0, numerals);
    }

//    private Document getDocument(String path) throws IOException {
//        try {
//            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
//            InputStream in = getResourceAsStream(path);
//
//            Document doc = builder.parse(in);
//            in.close();
//
//            doc.normalizeDocument();
//            return doc;
//        } catch (ParserConfigurationException | SAXException e) {
//            throw new IOException("Failed to load XML.", e);
//        }
//    }

}
