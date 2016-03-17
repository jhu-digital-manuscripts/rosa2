package rosa.archive.core.serialize;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import rosa.archive.model.BookScene;
import rosa.archive.model.NarrativeTagging;

/**
 * @see rosa.archive.core.serialize.NarrativeTaggingSerializer
 */
public class NarrativeTaggingSerializerTest extends BaseSerializerTest<NarrativeTagging> {

    @Before
    public void setup() {
        serializer = new NarrativeTaggingSerializer();
    }

    @Test
    public void readTest() throws IOException {
        readCSVTest();
//        readTxtTest();
    }

    public void readCSVTest() throws IOException {
        NarrativeTagging tagging = loadResource(COLLECTION_NAME, BOOK_NAME, "LudwigXV7.nartag.csv");
        assertNotNull(tagging);

        List<BookScene> scenes = tagging.getScenes();
        assertNotNull(scenes);
        assertEquals(341, scenes.size());

        // g8c,10v.d,1,10v.d,38,(SS57) Il ont pliens cleres fontainnes,0,1381
        BookScene scene = scenes.get(43);
        assertEquals("g8c", scene.getId());
        assertEquals("10v", scene.getStartPage());
        assertEquals("d", scene.getStartPageCol());
        assertEquals(1, scene.getStartLineOffset());
        assertEquals("10v", scene.getEndPage());
        assertEquals("d", scene.getEndPageCol());
        assertEquals(38, scene.getEndLineOffset());
    }

//    public void readTxtTest() throws IOException {
//        NarrativeTagging tagging = loadResource("data/Ferrell/Ferrell.nartag.txt");
//        assertNotNull(tagging);
//
//        List<BookScene> scenes = tagging.getScenes();
//        assertNotNull(scenes);
//        assertEquals(73, scenes.size());
//
//        BookScene scene = scenes.get(72);
//        assertNotNull(scene);
//        assertEquals("G16a", scene.getId());
//        assertEquals("19r", scene.getStartPage());
//        assertEquals("b", scene.getStartPageCol());
//        assertEquals(39, scene.getStartLineOffset());
//        assertEquals("19v", scene.getEndPage());
//        assertEquals("c", scene.getEndPageCol());
//        assertEquals(19, scene.getEndLineOffset());
//        assertEquals(3402, scene.getStartCriticalEdition());
//        assertEquals("Mais venus qui touzdiz guerroye", scene.getStartTranscription());
//        assertTrue(scene.isCorrect());
//    }

    @Test
    public void writeTest() throws IOException {
        writeObjectAndGetContent(createNartag());
    }

    @Override
    protected NarrativeTagging createObject() {
        return createNartag();
    }

    /**
     * @return a NarrativeTagging object to test the write method
     */
    private NarrativeTagging createNartag() {
        NarrativeTagging tagging = new NarrativeTagging();

//        tagging.setId("NarrativeTaggingID");

        List<BookScene> scenes = tagging.getScenes();
        for (int i = 0; i < 10; i++) {
            BookScene scene = new BookScene();

            scene.setId("SceneId" + i);
            scene.setStartPage(i + "r");
            scene.setStartPageCol("a");
            scene.setStartLineOffset(2);
            scene.setEndPage((i + 1) + "r");
            scene.setEndPageCol("b");
            scene.setEndLineOffset(12);
            scene.setCorrect(true);
            scene.setStartCriticalEdition(2);
            scene.setStartTranscription("This is the start of the transcription.");

            scenes.add(scene);
        }

        return tagging;
    }

}
