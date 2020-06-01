package rosa.archive.core.serialize;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Errata;
import rosa.archive.model.aor.Location;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.MarginaliaLanguage;
import rosa.archive.model.aor.Mark;
import rosa.archive.model.aor.Position;
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
        assertEquals("cacommodato", e.getReferencedText());
        assertEquals("acommodato", e.getAmendedText());

        // <mark name="plus_sign" method="pen" place="intext" language="IT" text="Arguto"/>
        Mark m1 = new Mark("mark,35", "Arguto", "plus_sign", "pen", "IT", Location.INTEXT);
        m1.setGeneratedId(true);
        // <mark name="dash" method="pen" place="right_margin"/>
        Mark m2 = new Mark("mark,1", "", "dash", "pen", "", Location.RIGHT_MARGIN);
        m2.setGeneratedId(true);
        // fake mark
        Mark m3 = new Mark(null, "fake text", "moo", "method", "lang", null);
        

        assertTrue(page.getMarks().contains(m1));
        assertTrue(page.getMarks().contains(m2));
        assertFalse(page.getMarks().contains(m3));
        
        // <symbol name="Sun" place="left_margin"/>
        Symbol s1 = new Symbol("symbol,0", "", "Sun", "", Location.LEFT_MARGIN);
        s1.setGeneratedId(true);
        // not present in document
        Symbol s2 = new Symbol(null, "fake text", "moo method", "lang", null);
        assertTrue(page.getSymbols().contains(s1));
        assertFalse(page.getSymbols().contains(s2));

        // <underline method="pen" type="straight" language="IT" text="Arguto, &amp;"/>
        Underline u1 = new Underline("underline,25", "Arguto, &", "pen", "straight", "IT");
        u1.setGeneratedId(true);
        // fake underline
        Underline u2 = new Underline(null, "fake text", "moomethod", "asdf", "lang");
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

    // Note IDs of annotations take special values based of Page#, annotation type, and annotation order
    @Override
    protected AnnotatedPage createObject() {
        AnnotatedPage page = new AnnotatedPage();

        page.setPage("1");
        page.setPagination("???");
        page.setReader("JUnit");
        page.setSignature("~~Signature~~");

        page.setMarks(Arrays.asList(
                new Mark("1_mark_0", "", "plus_sign", "pen", "EN", Location.HEAD),
                new Mark("1_mark_1", "", "plus_sign", "pen", "EN", Location.HEAD),
                new Mark("1_mark_2", "", "pen_trial", "pen", "EN", Location.HEAD)
        ));
        page.setSymbols(Arrays.asList(
                new Symbol("1_symbol_0", "", "Mars", "EN", Location.RIGHT_MARGIN),
                new Symbol("1_symbol_1", "", "Mars", "EN", Location.RIGHT_MARGIN),
                new Symbol("1_symbol_2", "", "Sun", "EN", Location.LEFT_MARGIN)
        ));
        page.setErrata(Collections.singletonList(new Errata("1_errata_0", "EN", "error text", "good text")));

        List<Marginalia> margs = new ArrayList<>();
        page.setMarginalia(margs);

        Marginalia m1 = new Marginalia();
        m1.setTranslation("Marginalia 1 translation");
        m1.setLocation(Location.TAIL);
        m1.setId("1_marginalia_0");

        List<MarginaliaLanguage> langs = new ArrayList<>();
        m1.setLanguages(langs);

        MarginaliaLanguage l1 = new MarginaliaLanguage();
        l1.setLang("EN");

        List<Position> poss = new ArrayList<>();
        l1.setPositions(poss);

        Position pos = new Position();
        poss.add(pos);

        pos.setOrientation(0);
        pos.setPlace(Location.INTEXT);
        pos.setTexts(Collections.singletonList("Text of Marginalia in pos1"));
        pos.setPeople(Arrays.asList("Jim", "Sayeed", "Mark"));
        pos.setBooks(Arrays.asList("Dune", "Foundation", "Hyperion"));

        return page;
    }

    /**
     * Test that the AOR parser expands XML entities based on the DTD definitions.
     *
     * @throws Exception .
     */
    @Test
    public void resolvingXmlEntitiesTest() throws Exception {
        String data = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                "<!DOCTYPE transcription SYSTEM \"http://www.livesandletters.ac.uk/schema/aor_20141023.dtd\">\n" +
                "<transcription xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.livesandletters.ac.uk/schema/aor2_18112016.xsd\">\n" +
                "    <page filename=\"BLC120b4.016r.tif\" pagination=\"16\" reader=\"John Dee\"/>\n" +
                "    <annotation>\n" +
                "        <marginalia hand=\"Italian\" method=\"pen\">\n" +
                "            <language ident=\"EN\">\n" +
                "                <position place=\"tail\" book_orientation=\"0\">\n" +
                "                    <marginalia_text>&Sun; - 1/2</marginalia_text>\n" +        // Entity here :: &Sun;  -->   ☉
                "                    <symbol_in_text name=\"Sun\"/>\n" +                        //   - Defined only in DTD
                "                </position>\n" +
                "            </language>\n" +
                "        </marginalia>\n" +
                "        <marginalia hand=\"Italian\" anchor_text=\"Adum&aacute;\" method=\"pen\">\n" +         // á
                "            <language ident=\"LA\">\n" +
                "                <position place=\"right_margin\" book_orientation=\"0\">\n" +
                "                    <marginalia_text>\n" +
                "                        Nota quod dicat Adum&aacute;, tantu[m], no[n] adiecta particula illa Voarch[adumia]:\n" +
                "                    </marginalia_text>\n" +
                "                    <person name=\"Adum&aacute;\"/>\n" +
                "                    <book title=\"Voarchadumia\"/>\n" +
                "                </position>\n" +
                "            </language>\n" +
                "            <translation>Note what Aduma says, only that particular was not aimed at Voarchadumia:</translation>\n" +
                "        </marginalia>\n" +
                "    </annotation>\n" +
                "</transcription>";

        List<String> errors = new ArrayList<>();
        InputStream in = new ByteArrayInputStream(data.getBytes("UTF-8"));

        AnnotatedPage obj = serializer.read(in, errors);

        assertNotNull(obj);
        assertTrue(errors.isEmpty());

        assertFalse(obj.getMarginalia().isEmpty());
        assertEquals(2, obj.getMarginalia().size());

        Position pos = obj.getMarginalia().get(0).getLanguages().get(0).getPositions().get(0);
        // Marginalia position must have text and symbol
        String text = pos.getTexts().get(0);
        assertTrue("Text should contain entity \"&Sun;\" or its resolved form \"☉\"\n\tInstead found \"" + text + "\"",
                text.contains("☉") || text.contains("&Sun;"));

        String otherPerson = obj.getMarginalia().get(1).getLanguages().get(0).getPositions().get(0).getPeople().get(0);
        assertTrue("Name of person should contain \"á\", instead found \"" + otherPerson + '"',
                otherPerson.contains("á"));
    }

}
