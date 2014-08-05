package rosa.archive.model;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @see rosa.archive.model.NarrativeSections
 */
public class NarrativeSectionsTest {
    private static final int MAX_SCENES = 10;

    private NarrativeSections sections;

    @Before
    public void setup() {
        this.sections = new NarrativeSections();

        List<NarrativeScene> scenes = new ArrayList<>();
        for (int i = 0; i < MAX_SCENES; i++) {
            NarrativeScene scene = mock(NarrativeScene.class);
            when(scene.getId()).thenReturn(String.valueOf(i));

            scenes.add(scene);
        }
        sections.setScenes(scenes);
    }

    @Test
    public void correctIndexReturnedForValidId() {
        String[] goodId = { "2", "5", "9" };
        for (String id : goodId) {
            int index = sections.findIndexOfSceneById(id);
            assertTrue(index > 0);
            assertTrue(index < MAX_SCENES);
        }

        String[] badId = { "-2", "15", "100" };
        for (String id : badId) {
            assertEquals(-1, sections.findIndexOfSceneById(id));
        }
    }

}
