package rosa.archive.core.serialize;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.BookText;
import rosa.archive.model.meta.BiblioData;
import rosa.archive.model.meta.MultilangMetadata;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MultilangMetadataSerializerTest extends BaseSerializerTest {

    private MultilangMetadataSerializer serializer;

    @Before
    public void setup() {
        super.setup();
        serializer = new MultilangMetadataSerializer();
    }

    @Test
    public void readTest() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<book>\n" +
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
                "        <text id=\"1\">\n" +
                "            <title>title</title>\n" +
                "            <textId>rose</textId>\n" +
                "            <pages start=\"1r\" end=\"100r\">100</pages>\n" +
                "            <illustrations>1</illustrations>\n" +
                "            <linesPerColumn>2</linesPerColumn>\n" +
                "            <columnsPerPage>3</columnsPerPage>\n" +
                "            <leavesPerGathering>4</leavesPerGathering>\n" +
                "        </text>\n" +
                "        <text id=\"2\">\n" +
                "            <title>title</title>\n" +
                "            <textId>not rose</textId>\n" +
                "            <pages start=\"100v\" end=\"300r\">200</pages>\n" +
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
                "            <author>Author</author>\n" +
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
                "            <author>Author</author>\n" +
                "        </bibliography>\n" +
                "    </bibliographies>\n" +
                "</book>";

        List<String> errors = new ArrayList<>();
        MultilangMetadata metadata = null;
        try (InputStream in = new ByteArrayInputStream(xml.getBytes("UTF-8"))) {
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
        assertEquals("Author", biblio.getAuthors()[0]);
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
        assertEquals("rose", text.getTextId());
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
        MultilangMetadata metadata = null;
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

    @Test (expected = UnsupportedOperationException.class)
    public void writeTest() throws Exception {
        serializer.write(null, null);
    }

    private BookMetadata createMetadata(String lang) {
        BookMetadata metadata = new BookMetadata();
        metadata.setId("Test.ID");

        metadata.setCommonName("Common Name");
        metadata.setCurrentLocation("Current Location");
        metadata.setDate("Today's date.");
        metadata.setWidth(1000);
        metadata.setHeight(2000);
        metadata.setNumberOfIllustrations(42);
        metadata.setNumberOfPages(100);
        metadata.setDimensions("1000x2000");
        metadata.setMaterial("Some Material");
        metadata.setOrigin("Origin");
        metadata.setRepository("Repository");
        metadata.setShelfmark("On the shelf");
        metadata.setYearEnd(300);
        metadata.setYearStart(100);
        metadata.setType("The type");

        List<BookText> bookTextList = new ArrayList<>();
        for (int i = 0; i < 1; i++) {
            BookText text = new BookText();
            text.setId("ID" + i);
            text.setFirstPage("Page " + i);
            text.setLastPage("Page " + (i+1));
            text.setColumnsPerPage(2);
            text.setLeavesPerGathering(6);
            text.setLinesPerColumn(45);
            text.setNumberOfIllustrations(42);
            text.setNumberOfPages(36);
            text.setTitle("Title Title");

            bookTextList.add(text);
        }
        metadata.setTexts(bookTextList.toArray(new BookText[bookTextList.size()]));

        return metadata;
    }

}