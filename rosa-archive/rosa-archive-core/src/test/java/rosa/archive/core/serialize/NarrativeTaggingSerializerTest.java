package rosa.archive.core.serialize;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.BookScene;
import rosa.archive.model.NarrativeTagging;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @see rosa.archive.core.serialize.NarrativeTaggingSerializer
 */
public class NarrativeTaggingSerializerTest {

    private NarrativeTaggingSerializer serializer;

    @Before
    public void setup() {
        this.serializer = new NarrativeTaggingSerializer();
    }

    @Test
    public void readCSVTest() throws IOException {
        final String testFile = "data/LudwigXV7/LudwigXV7.nartag.csv";

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(testFile)) {
            NarrativeTagging tagging = serializer.read(in);
            assertNotNull(tagging);

            List<BookScene> scenes = tagging.getScenes();
            assertNotNull(scenes);
            assertEquals(81, scenes.size());

            //g11c,19r.b,31,19v.c,29,,0,2809
            BookScene scene = scenes.get(80);
            assertEquals("g11c", scene.getId());
            assertEquals("19r", scene.getStartPage());
            assertEquals("b", scene.getStartPageCol());
            assertEquals(31, scene.getStartLineOffset());
            assertEquals("19v", scene.getEndPage());
            assertEquals("c", scene.getEndPageCol());
            assertEquals(29, scene.getEndLineOffset());
        }
    }

    @Test
    public void readTxtTest() throws IOException {
        final String testFile = "data/Ferrell/Ferrell.nartag.txt";

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(testFile)) {
            NarrativeTagging tagging = serializer.read(in);
            assertNotNull(tagging);

            List<BookScene> scenes = tagging.getScenes();
            assertNotNull(scenes);
            assertEquals(73, scenes.size());

            BookScene scene = scenes.get(72);
            assertNotNull(scene);
            assertEquals("G16a", scene.getId());
            assertEquals("19r", scene.getStartPage());
            assertEquals("b", scene.getStartPageCol());
            assertEquals(39, scene.getStartLineOffset());
            assertEquals("19v", scene.getEndPage());
            assertEquals("c", scene.getEndPageCol());
            assertEquals(19, scene.getEndLineOffset());
            assertEquals(3402, scene.getStartCriticalEdition());
            assertEquals("Mais venus qui touzdiz guerroye", scene.getStartTranscription());
            assertTrue(scene.isCorrect());
        }
    }

    @Test (expected = UnsupportedOperationException.class)
    public void writeTest() throws IOException {
        OutputStream out = mock(OutputStream.class);
        serializer.write(new NarrativeTagging(), out);
    }

}
