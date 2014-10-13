package rosa.archive.core.serialize;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.Illustration;
import rosa.archive.model.IllustrationTagging;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @see rosa.archive.core.serialize.IllustrationTaggingSerializer
 */
public class IllustrationTaggingSerializerTest extends BaseSerializerTest {

    private Serializer<IllustrationTagging> serializer;

    @Before
    public void setup() {
        super.setup();
        serializer = new IllustrationTaggingSerializer(config);
    }

    @Test
    public void readTest() throws IOException {
        final String testFile = "data/Walters143/Walters143.imagetag.csv";

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(testFile)) {
            IllustrationTagging tagging = serializer.read(in, errors);
            assertNotNull(tagging);
            assertEquals(29, tagging.size());

            Illustration illustration = tagging.getIllustrationData(28);
            assertNotNull(illustration);

            assertEquals("29", illustration.getId());
            assertEquals("26v", illustration.getPage());
            assertNotNull(illustration.getTitles());
            assertEquals(1, illustration.getTitles().length);
            assertTrue(StringUtils.isBlank(illustration.getTextualElement()));
            assertTrue(illustration.getInitials().startsWith("Blue initial"));
            assertNotNull(illustration.getCharacters());
            assertEquals(1, illustration.getCharacters().length);
            assertTrue(illustration.getCostume().startsWith("Jalousie wears"));
            assertEquals("Ladder, trowel, hod, and hammer", illustration.getObject());
            assertEquals("Grass ground beneath castle", illustration.getLandscape());
            assertTrue(illustration.getArchitecture().startsWith("Crenellated wall"));
            assertEquals("Diaper pattern background", illustration.getOther());

            for (Illustration ill : tagging) {
                assertNotNull(ill.getId());
                assertNotNull(ill.getPage());
                assertNotNull(ill.getTitles());
            }
        }
    }

    @Test
    public void writeTest() throws IOException {
        IllustrationTagging tagging = createTagging();
        serializer.write(tagging, System.out);
    }

    private IllustrationTagging createTagging() {
        IllustrationTagging tagging = new IllustrationTagging();

        tagging.setId("TaggingID");
        for (int i = 0; i < 10; i++) {
            Illustration ill = new Illustration();
            ill.setId("IllID" + i);
            ill.setPage("Page " + i);
            ill.setTitles(new String[] {"Title1", "Title2"});
            ill.setTextualElement("TextualElement");
            ill.setCharacters(new String[] {"char1", "char2"});
            ill.setOther("OtherOtherOther");

            tagging.addIllustrationData(ill);
        }

        return tagging;
    }

}
