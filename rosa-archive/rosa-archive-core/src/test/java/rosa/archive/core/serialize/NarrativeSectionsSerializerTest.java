package rosa.archive.core.serialize;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.NarrativeScene;
import rosa.archive.model.NarrativeSections;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @see rosa.archive.core.serialize.NarrativeSectionsSerializer
 */
public class NarrativeSectionsSerializerTest extends BaseSerializerTest {

    private Serializer<NarrativeSections> serializer;

    @Before
    public void setup() {
        super.setup();
        serializer = new NarrativeSectionsSerializer(config);
    }

    @Test
    public void readTest() throws IOException {
        final String testFile = "data/narrative_sections.csv";

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(testFile)) {
            NarrativeSections sections = serializer.read(in, errors);
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
    }

    @Test
    public void writeTest() throws IOException {
        NarrativeSections sections = createNarrativeSections();
//        serializer.write(sections, System.out);

        // TODO
    }

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
