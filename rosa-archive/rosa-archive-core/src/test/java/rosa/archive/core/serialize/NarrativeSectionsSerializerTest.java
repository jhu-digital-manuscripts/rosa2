package rosa.archive.core.serialize;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import rosa.archive.model.NarrativeScene;
import rosa.archive.model.NarrativeSections;

/**
 * @see rosa.archive.core.serialize.NarrativeSectionsSerializer
 */
public class NarrativeSectionsSerializerTest extends BaseSerializerTest<NarrativeSections> {

    @Before
    public void setup() {
        serializer = new NarrativeSectionsSerializer();
    }

    @Test
    public void readTest() throws IOException {
        NarrativeSections sections = loadResource(COLLECTION_NAME, null, "narrative_sections.csv");
        assertNotNull(sections);

        List<NarrativeScene> scenes = sections.asScenes();
        assertNotNull(scenes);
        // There is one line that is missing Lecoy
        assertEquals(341, scenes.size());

        NarrativeScene scene = scenes.get(0);
        assertEquals("G 1a", scene.getId());
        assertEquals(1, scene.getCriticalEditionStart());
        assertEquals(20, scene.getCriticalEditionEnd());
        assertEquals(1, scene.getRel_line_start());
        assertEquals(20, scene.getRel_line_end());
        assertEquals("Preface", scene.getDescription());
    }

    @Test
    public void writeTest() throws IOException {
        writeObjectAndGetContent(createNarrativeSections());
    }

    /**
     * @return a NarrativeSections object to test the write method
     */
    private NarrativeSections createNarrativeSections() {
        NarrativeSections sections = new NarrativeSections();

        List<NarrativeScene> scenes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            NarrativeScene scene = new NarrativeScene();

            scene.setId("ID" + i);
            scene.setCriticalEditionStart(i);
            scene.setCriticalEditionEnd(i + 2);
            scene.setRel_line_start(i);
            scene.setRel_line_end(i + 2);
            scene.setDescription("This is a description.");

            scenes.add(scene);
        }
        sections.setScenes(scenes);

        return sections;
    }

}
