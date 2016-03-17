package rosa.archive.core.serialize;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.Illustration;
import rosa.archive.model.IllustrationTagging;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @see rosa.archive.core.serialize.IllustrationTaggingSerializer
 */
public class IllustrationTaggingSerializerTest extends BaseSerializerTest<IllustrationTagging> {

    @Before
    public void setup() {
        serializer = new IllustrationTaggingSerializer();
    }

    @Test
    public void readTest() throws IOException {
        IllustrationTagging tagging = loadResource(COLLECTION_NAME, BOOK_NAME, "LudwigXV7.imagetag.csv");
        assertNotNull(tagging);
        assertEquals(101, tagging.size());

        Illustration illustration = tagging.getIllustrationData(28);
        assertNotNull(illustration);

        assertEquals("29", illustration.getId());
        assertEquals("19r", illustration.getPage());
        assertNotNull(illustration.getTitles());
        assertEquals(1, illustration.getTitles().length);
        assertTrue(StringUtils.isBlank(illustration.getTextualElement()));
        assertTrue(illustration.getInitials().isEmpty());
        assertNotNull(illustration.getCharacters());
        assertEquals(3, illustration.getCharacters().length);
        assertEquals("Chaperon and bourrelet", illustration.getCostume());
        assertEquals("Club", illustration.getObject());
        assertTrue(illustration.getLandscape().startsWith("Figures stand within wattle fence"));
        assertTrue(illustration.getArchitecture().isEmpty());
        assertTrue(illustration.getOther().isEmpty());

        for (Illustration ill : tagging) {
            assertNotNull(ill.getId());
            assertNotNull(ill.getPage());
            assertNotNull(ill.getTitles());
        }
    }

    @Test
    public void writeTest() throws IOException {
        List<String> lines = writeObjectAndGetWrittenLines(createTagging());

        assertNotNull(lines);
        assertEquals(11, lines.size());
        assertEquals("id,Folio,Illustration title,Textual elements,Initials,Characters,Costume,Objects,Landscape,Architecture,Other",
                lines.get(0));
        assertTrue(lines.contains("IllID1,Page 1,\"Title1,Title2\",TextualElement,,\"char1,char2\",,,,,OtherOtherOther"));
        assertTrue(lines.contains("IllID8,Page 8,\"Title1,Title2\",TextualElement,,\"char1,char2\",,,,,OtherOtherOther"));
    }

    @Override
    protected IllustrationTagging createObject() {
        return createTagging();
    }

    /**
     * @return an IllustrationTagging object to test the write method.
     */
    private IllustrationTagging createTagging() {
        IllustrationTagging tagging = new IllustrationTagging();

//        tagging.setId("TaggingID");
        for (int i = 0; i < 10; i++) {
            Illustration ill = new Illustration();
            ill.setId("IllID" + i);
            ill.setPage("Page " + i);
            ill.setTitles(new String[] {"Title1", "Title2"});
            ill.setTextualElement("TextualElement");
            ill.setCharacters(new String[] {"char1", "char2"});
            ill.setOther("OtherOtherOther");
            ill.setObject("");
            ill.setCostume("");
            ill.setInitials("");
            ill.setLandscape("");
            ill.setArchitecture("");

            tagging.addIllustrationData(ill);
        }

        return tagging;
    }

}
