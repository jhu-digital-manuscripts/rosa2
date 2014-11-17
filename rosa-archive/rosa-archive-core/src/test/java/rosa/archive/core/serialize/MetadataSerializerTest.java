package rosa.archive.core.serialize;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.BookText;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MetadataSerializerTest extends BaseSerializerTest {

    private MetadataSerializer serializer;

    @Before
    public void setup() {
        super.setup();
        serializer = new MetadataSerializer();
    }

    @Test
    public void readTest() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                "<book illustrations=\"42\" pages=\"100\">\n" +
                "    <dimensions height=\"2000\" unit=\"mm\" width=\"1000\"/>\n" +
                "    <texts>\n" +
                "        <text columnsPerPage=\"2\" end=\"Page 1\" id=\"ID0\" illustrations=\"42\" leavesPerGathering=\"6\" linesPerColumn=\"45\" pages=\"36\" start=\"Page 0\" title=\"Title Title\"/>\n" +
                "    </texts>\n" +
                "    <bibliography lang=\"en\">\n" +
                "        <date end=\"300\" start=\"100\">Today's date.</date>\n" +
                "        <type>The type</type>\n" +
                "        <commonName>Common Name</commonName>\n" +
                "        <material>Some Material</material>\n" +
                "        <origin>Origin</origin>\n" +
                "        <currentLocation>Current Location</currentLocation>\n" +
                "        <repository>Repository</repository>\n" +
                "        <shelfmark>On the shelf</shelfmark>\n" +
                "    </bibliography>\n" +
                "    <bibliography lang=\"fr\">\n" +
                "        <date end=\"300\" start=\"100\">Today's date.</date>\n" +
                "        <type>The type</type>\n" +
                "        <commonName>Common Name</commonName>\n" +
                "        <material>Some Material</material>\n" +
                "        <origin>Origin</origin>\n" +
                "        <currentLocation>Current Location</currentLocation>\n" +
                "        <repository>Repository</repository>\n" +
                "        <shelfmark>On the shelf</shelfmark>\n" +
                "    </bibliography>\n" +
                "</book>";

        List<String> errors = new ArrayList<>();
        Map<String, BookMetadata> metadataMap = null;
        try (InputStream in = new ByteArrayInputStream(xml.getBytes("UTF-8"))) {
            metadataMap = serializer.read(in, errors);
        }

        assertNotNull(metadataMap);
        assertEquals(0, errors.size());
        assertEquals(2, metadataMap.size());
        assertTrue(metadataMap.containsKey("en"));
        assertTrue(metadataMap.containsKey("fr"));

    }

    @Test
    public void writeTest() throws Exception {
        BookMetadata metadata = createMetadata("en");
        BookMetadata metadataFr = createMetadata("fr");

        Map<String, BookMetadata> metadataMap = new HashMap<>();
        metadataMap.put("en", metadata);
        metadataMap.put("fr", metadataFr);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            serializer.write(metadataMap, out);
            String res = out.toString("UTF-8");
            assertNotNull(res);
            assertTrue(res.length() > 0);

            List<String> lines = Arrays.asList(res.split("\n"));
            assertNotNull(lines);
            assertEquals(27, lines.size());

            assertTrue(lines.contains("<book illustrations=\"42\" pages=\"100\">"));
            assertTrue(lines.contains("        <material>Some Material</material>"));
            assertTrue(lines.contains("    <bibliography lang=\"fr\">"));
        }
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