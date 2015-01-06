package rosa.archive.core.serialize;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.BookText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @see rosa.archive.core.serialize.BookMetadataSerializer
 */
public class BookMetadataSerializerTest extends BaseSerializerTest<BookMetadata> {

    @Before
    public void setup() {
        serializer = new BookMetadataSerializer();
    }

    @Test
    public void readTest() throws IOException {
        final String testFile = "LudwigXV7.description_en.xml";

        List<String> errors = new ArrayList<>();

        BookMetadata metadata = loadResource(COLLECTION_NAME, BOOK_NAME, testFile, errors);
        assertNotNull(metadata);

        assertEquals("15th century", metadata.getDate());
        assertEquals(1400, metadata.getYearStart());
        assertEquals(1500, metadata.getYearEnd());
        assertEquals("Los Angeles", metadata.getCurrentLocation());
        assertEquals("J. Paul Getty Museum", metadata.getRepository());
        assertNotNull(metadata.getShelfmark());
        assertNotNull(metadata.getOrigin());
        assertEquals("manuscript", metadata.getType());
        assertNotNull(metadata.getDimensions());
        assertEquals(260, metadata.getWidth());
        assertEquals(370, metadata.getHeight());
        assertTrue(metadata.getNumberOfIllustrations() > -1);
        assertTrue(metadata.getNumberOfPages() > -1);
        assertNotNull(metadata.getCommonName());
        assertNotNull(metadata.getMaterial());

        assertNotNull(metadata.getTexts());
        assertEquals(1, metadata.getTexts().length);
    }

    @Test
    public void writeTest() throws IOException {
        List<String> lines = writeObjectAndGetWrittenLines(createMetadata());

        assertEquals("<TEI xmlns=\"http://www.tei-c.org/ns/1.0\" version=\"5.0\">", lines.get(1));
        assertEquals("    <teiheader>", lines.get(2));
        assertEquals("    </teiheader>", lines.get(lines.size() - 2));
        assertEquals("</TEI>", lines.get(lines.size() - 1));

        assertEquals("        <sourceDesc>", lines.get(3));
        assertEquals("                <title>Title</title>", lines.get(5));
        assertEquals("                        <height unit=\"mm\">2000</height>", lines.get(15));
        assertEquals("            <msDesc>", lines.get(20));
        assertEquals("                        <locus from=\"Page 0\" to=\"Page 1\">Page 0-Page 1</locus>",
                    lines.get(28));
        assertEquals("                        <note type=\"linesPerColumn\">45</note>",
                    lines.get(50));
    }

    private BookMetadata createMetadata() {
        BookMetadata metadata = new BookMetadata();
        metadata.setId("Test.ID");

        metadata.setCommonName("Common Name");
        metadata.setTitle("Title");
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
        metadata.setDimensionUnits("mm");

        List<BookText> bookTextList = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
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
