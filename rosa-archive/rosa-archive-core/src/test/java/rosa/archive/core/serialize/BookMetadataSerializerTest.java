package rosa.archive.core.serialize;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.BookText;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @see rosa.archive.core.serialize.BookMetadataSerializer
 */
public class BookMetadataSerializerTest extends BaseSerializerTest {

    private Serializer<BookMetadata> serializer;

    @Before
    public void setup() {
        super.setup();
        serializer = new BookMetadataSerializer(config);
    }

    @Test
    public void readTest() throws IOException {
        final String testFile = "data/Walters143/Walters143.description_en.xml";

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(testFile)) {
            BookMetadata metadata = serializer.read(in, errors);
            assertNotNull(metadata);

            assertEquals("14th century", metadata.getDate());
            assertEquals(1400, metadata.getYearStart());
            assertEquals(1300, metadata.getYearEnd());
            assertEquals("Baltimore, MD", metadata.getCurrentLocation());
            assertEquals("Walters Art Museum", metadata.getRepository());
            assertNotNull(metadata.getShelfmark());
            assertNotNull(metadata.getOrigin());
            assertEquals("manuscript", metadata.getType());
            assertNotNull(metadata.getDimensions());
            assertEquals(216, metadata.getWidth());
            assertEquals(300, metadata.getHeight());
            assertTrue(metadata.getNumberOfIllustrations() > -1);
            assertTrue(metadata.getNumberOfPages() > -1);
            assertNotNull(metadata.getCommonName());
            assertNotNull(metadata.getMaterial());

            assertNotNull(metadata.getTexts());
            assertEquals(1, metadata.getTexts().length);
        }
    }

    @Test
    public void writeTest() throws IOException {

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

        serializer.write(metadata, System.out);

    }

}
