package rosa.archive.core.serialize;

import org.junit.Before;
import org.junit.Test;

import rosa.archive.model.BiblioData;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.BookText;
import rosa.archive.model.ObjectRef;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BookMetadataSerializerTest extends BaseSerializerTest<BookMetadata> {
    private final static String bigXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
            "<book>\n" +
            "    <license>\n" +
            "        <url/>\n" +
            "        <logo/>\n" +
            "    </license>\n" +
            "    <illustrations>10</illustrations>\n" +
            "    <totalPages>8</totalPages>\n" +
            "    <dimensions units=\"mm\">\n" +
            "        <width>100</width>\n" +
            "        <height>200</height>\n" +
            "    </dimensions>\n" +
            "    <dates>\n" +
            "        <startDate>1900</startDate>\n" +
            "        <endDate>1950</endDate>\n" +
            "    </dates>\n" +
            "    <texts>\n" +
            "        <text>\n" +
            "            <language>fr</language>\n" +
            "            <title>title</title>\n" +
            "            <pages end=\"100r\" start=\"1r\">100</pages>\n" +
            "            <illustrations>1</illustrations>\n" +
            "            <linesPerColumn>2</linesPerColumn>\n" +
            "            <columnsPerPage>3</columnsPerPage>\n" +
            "            <leavesPerGathering>4</leavesPerGathering>\n" +
            "        </text>\n" +
            "        <text>\n" +
            "            <language>fr</language>\n" +
            "            <title>title</title>\n" +
            "            <pages end=\"300r\" start=\"100v\">200</pages>\n" +
            "            <illustrations>0</illustrations>\n" +
            "            <linesPerColumn>0</linesPerColumn>\n" +
            "            <columnsPerPage>0</columnsPerPage>\n" +
            "            <leavesPerGathering>0</leavesPerGathering>\n" +
            "        </text>\n" +
            "    </texts>\n" +
            "    <bibliographies>\n" +
            "        <bibliography lang=\"en\">\n" +
            "            <title>Book Title</title>\n" +
            "            <dateLabel>Date String</dateLabel>\n" +
            "            <type>Type String</type>\n" +
            "            <commonName>Common Name String</commonName>\n" +
            "            <material>Material String</material>\n" +
            "            <origin>Origin String</origin>\n" +
            "            <currentLocation>Current Location String</currentLocation>\n" +
            "            <repository>Repository String</repository>\n" +
            "            <shelfmark>Shelfmark String</shelfmark>\n" +
            "            <detail>Bibliographic Details</detail>\n" +
            "            <author>\n" +
            "                <name>Author</name>\n" +
            "                <id>https://example.com/authorial</id>\n" +
            "            </author>\n" +
            "            <reader>\n" +
            "                <name>Moo Jones</name>\n" +
            "                <id>https://example.com/reader/moo</id>\n" +
            "            </reader>\n" +
            "            <website>https://example.com/aor</website>\n" +
            "            <note>Note</note>\n" +
            "        </bibliography>\n" +
            "        <bibliography lang=\"fr\">\n" +
            "            <title>Book Title</title>\n" +
            "            <dateLabel>Date String</dateLabel>\n" +
            "            <type>Type String</type>\n" +
            "            <commonName>Common Name String</commonName>\n" +
            "            <material>Material String</material>\n" +
            "            <origin>Origin String</origin>\n" +
            "            <currentLocation>Current Location String</currentLocation>\n" +
            "            <repository>Repository String</repository>\n" +
            "            <shelfmark>Shelfmark String</shelfmark>\n" +
            "            <detail>Bibliographic Details</detail>\n" +
            "            <author>\n" +
            "                <name>Author</name>\n" +
            "                <id>https://example.com/authorial</id>\n" +
            "            </author>\n" +
            "            <reader>\n" +
            "                <name>Moo Jones</name>\n" +
            "                <id>https://example.com/reader/moo</id>\n" +
            "            </reader>\n" +
            "            <website>https://example.com/aor</website>\n" +
            "        </bibliography>\n" +
            "    </bibliographies>\n" +
            "</book>";

    @Before
    public void setup() {
        serializer = new BookMetadataSerializer();
    }

    @Test
    public void readTest() throws IOException {

        List<String> errors = new ArrayList<>();
        BookMetadata metadata = null;
        try (InputStream in = new ByteArrayInputStream(bigXml.getBytes("UTF-8"))) {
            metadata = serializer.read(in, errors);
        }

        assertNotNull(metadata);
        assertEquals(0, errors.size());
        assertNotNull(metadata.getBiblioDataMap());
        assertEquals(2, metadata.getBiblioDataMap().size());
        assertTrue(metadata.getBiblioDataMap().containsKey("en"));
        assertTrue(metadata.getBiblioDataMap().containsKey("fr"));

        BiblioData biblio = metadata.getBiblioDataMap().get("fr");
        assertNotNull(biblio);
        assertEquals("fr", biblio.getLanguage());
        assertEquals("Book Title", biblio.getTitle());
        assertEquals("Date String", biblio.getDateLabel());
        assertEquals("Type String", biblio.getType());
        assertEquals("Common Name String", biblio.getCommonName());
        assertEquals("Material String", biblio.getMaterial());
        assertEquals("Origin String", biblio.getOrigin());
        assertEquals("Current Location String", biblio.getCurrentLocation());
        assertEquals("Repository String", biblio.getRepository());
        assertEquals("Shelfmark String", biblio.getShelfmark());
        assertNotNull(biblio.getDetails());
        assertEquals(1, biblio.getDetails().length);
        assertEquals("Bibliographic Details", biblio.getDetails()[0]);
        assertNotNull(biblio.getAuthors());
        assertEquals(1, biblio.getAuthors().length);
        assertEquals("Author", biblio.getAuthors()[0].getName());
        assertEquals("Moo Jones", biblio.getReaders()[0].getName());
        assertNotNull(biblio.getNotes());
        assertEquals(0, biblio.getNotes().length);

        assertEquals(10, metadata.getNumberOfIllustrations());
        assertEquals(8, metadata.getNumberOfPages());
        assertEquals(100, metadata.getWidth());
        assertEquals(200, metadata.getHeight());
        assertEquals("mm", metadata.getDimensionUnits());
        assertEquals("100mm x 200mm", metadata.getDimensionsString());
        assertEquals(1900, metadata.getYearStart());
        assertEquals(1950, metadata.getYearEnd());
        assertNotNull(metadata.getBookTexts());
        assertEquals(2, metadata.getBookTexts().size());

        BookText text = metadata.getBookTexts().get(0);
        assertNotNull(text);
        assertEquals("title", text.getTitle());
        assertEquals(100, text.getNumberOfPages());
        assertEquals("1r", text.getFirstPage());
        assertEquals("100r", text.getLastPage());
        assertEquals(1, text.getNumberOfIllustrations());
        assertEquals(2, text.getLinesPerColumn());
        assertEquals(3, text.getColumnsPerPage());
        assertEquals(4, text.getLeavesPerGathering());
    }

    @Test
    public void readMinimumTest() throws Exception {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<book>\n" +
                "    <bibliographies>\n" +
                "        <bibliography lang=\"en\">\n" +
                "            <commonName>Common Name String</commonName>\n" +
                "            <currentLocation>Current Location String</currentLocation>\n" +
                "            <repository>Repository String</repository>\n" +
                "            <shelfmark>Shelfmark String</shelfmark>\n" +
                "        </bibliography>\n" +
                "    </bibliographies>\n" +
                "</book>";

        List<String> errors = new ArrayList<>();
        BookMetadata metadata = null;
        try (InputStream in = new ByteArrayInputStream(xml.getBytes("UTF-8"))) {
            metadata = serializer.read(in, errors);
        }

        assertNotNull(metadata);
        assertEquals(0, errors.size());

        assertNotNull(metadata.getBiblioDataMap());
        assertEquals(1, metadata.getBiblioDataMap().size());
        assertTrue(metadata.getBiblioDataMap().containsKey("en"));

        BiblioData biblio = metadata.getBiblioDataMap().get("en");
        assertNotNull(biblio);
        assertEquals("en", biblio.getLanguage());
        assertEquals("", biblio.getTitle());
        assertEquals("", biblio.getDateLabel());
        assertEquals("", biblio.getType());
        assertEquals("Common Name String", biblio.getCommonName());
        assertEquals("", biblio.getMaterial());
        assertEquals("", biblio.getOrigin());
        assertEquals("Current Location String", biblio.getCurrentLocation());
        assertEquals("Repository String", biblio.getRepository());
        assertEquals("Shelfmark String", biblio.getShelfmark());
        assertNotNull(biblio.getDetails());
        assertEquals(0, biblio.getDetails().length);
        assertNotNull(biblio.getAuthors());
        assertEquals(0, biblio.getAuthors().length);
        assertNotNull(biblio.getNotes());
        assertEquals(0, biblio.getNotes().length);

        assertEquals(-1, metadata.getNumberOfIllustrations());
        assertEquals(-1, metadata.getNumberOfPages());
        assertEquals(-1, metadata.getWidth());
        assertEquals(-1, metadata.getHeight());
        assertEquals("", metadata.getDimensionUnits());
        assertEquals("-1 x -1", metadata.getDimensionsString());
        assertEquals(-1, metadata.getYearStart());
        assertEquals(-1, metadata.getYearEnd());
        assertNotNull(metadata.getBookTexts());
        assertEquals(0, metadata.getBookTexts().size());
    }

    @Test
    public void writeTest() throws IOException {
        List<String> expectedLines = Arrays.asList(bigXml.split("\n"));
        List<String> resultLines = writeObjectAndGetWrittenLines(createMetadata());

        // All lines in results are in expected lines
        for (String line : resultLines) {
            assertTrue(expectedLines.contains(line));
        }
    }

    @Override
    protected BookMetadata createObject() {
        return createMetadata();
    }

    private BookMetadata createMetadata() {
        BookMetadata metadata = new BookMetadata();

        metadata.setNumberOfIllustrations(10);
        metadata.setNumberOfPages(8);
        metadata.setDimensionUnits("mm");
        metadata.setWidth(100);
        metadata.setHeight(200);
        metadata.setYearStart(1900);
        metadata.setYearEnd(1950);
//        metadata.setLicenseUrl("http://example.org/license");

        BookText t1 = new BookText();
        t1.setTitle("title");
        t1.setFirstPage("1r");
        t1.setLastPage("100r");
        t1.setNumberOfPages(100);
        t1.setNumberOfIllustrations(1);
        t1.setLinesPerColumn(2);
        t1.setColumnsPerPage(3);
        t1.setLeavesPerGathering(4);
        t1.setLanguage("fr");
        metadata.getBookTexts().add(t1);

        BookText t2 = new BookText();
        t2.setTitle("title");
        t2.setFirstPage("100v");
        t2.setLastPage("300r");
        t2.setNumberOfPages(200);
        t2.setNumberOfIllustrations(0);
        t2.setLinesPerColumn(0);
        t2.setColumnsPerPage(0);
        t2.setLeavesPerGathering(0);
        t2.setLanguage("fr");
        metadata.getBookTexts().add(t2);

        BiblioData d2 = new BiblioData();
        d2.setLanguage("fr");
        d2.setTitle("Book Title");
        d2.setDateLabel("Date String");
        d2.setType("Type String");
        d2.setCommonName("Common Name String");
        d2.setMaterial("Material String");
        d2.setOrigin("Origin String");
        d2.setCurrentLocation("Current Location String");
        d2.setRepository("Repository String");
        d2.setShelfmark("Shelfmark String");
        d2.setDetails(new String[]{"Bibliographic Details"});
        d2.setAuthors(new ObjectRef[]{new ObjectRef("Author", "https://example.com/authorial")});
        d2.setReaders(new ObjectRef[]{new ObjectRef("Moo Jones", "https://example.com/reader/moo")});
        d2.setWebsites(new  String[] {"https://example.com/aor"});
        metadata.getBiblioDataMap().put("fr", d2);

        BiblioData d1 = new BiblioData();
        d1.setLanguage("en");
        d1.setTitle("Book Title");
        d1.setDateLabel("Date String");
        d1.setType("Type String");
        d1.setCommonName("Common Name String");
        d1.setMaterial("Material String");
        d1.setOrigin("Origin String");
        d1.setCurrentLocation("Current Location String");
        d1.setRepository("Repository String");
        d1.setShelfmark("Shelfmark String");
        d1.setDetails(new String[] {"Bibliographic Details"});
        d1.setAuthors(new ObjectRef[] {new ObjectRef("Author", "https://example.com/authorial")});
        d1.setReaders(new ObjectRef[]{new ObjectRef("Moo Jones", "https://example.com/reader/moo")});
        d1.setWebsites(new  String[] {"https://example.com/aor"});
        d1.setNotes(new String[] {"Note"});
        metadata.getBiblioDataMap().put("en", d1);

        return metadata;
    }

}