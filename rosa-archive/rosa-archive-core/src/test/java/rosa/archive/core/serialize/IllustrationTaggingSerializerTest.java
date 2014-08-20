package rosa.archive.core.serialize;

import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import rosa.archive.core.ArchiveCoreModule;
import rosa.archive.core.GuiceJUnitRunner;
import rosa.archive.core.GuiceJUnitRunner.GuiceModules;
import rosa.archive.model.Illustration;
import rosa.archive.model.IllustrationTagging;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @see rosa.archive.core.serialize.IllustrationTaggingSerializer
 */
@RunWith(GuiceJUnitRunner.class)
@GuiceModules({ArchiveCoreModule.class})
public class IllustrationTaggingSerializerTest extends BaseSerializerTest {

    @Inject
    private Serializer<IllustrationTagging> serializer;

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
        }
    }

    @Test (expected = UnsupportedOperationException.class)
    public void writeTest() throws IOException {
        OutputStream out = mock(OutputStream.class);
        serializer.write(new IllustrationTagging(), out);
    }

}
