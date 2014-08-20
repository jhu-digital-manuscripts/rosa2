package rosa.archive.core.serialize;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import rosa.archive.core.ArchiveCoreModule;
import rosa.archive.core.GuiceJUnitRunner;
import rosa.archive.core.GuiceJUnitRunner.GuiceModules;
import rosa.archive.model.NarrativeScene;
import rosa.archive.model.NarrativeSections;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * @see rosa.archive.core.serialize.NarrativeSectionsSerializer
 */
@RunWith(GuiceJUnitRunner.class)
@GuiceModules({ArchiveCoreModule.class})
public class NarrativeSectionsSerializerTest extends BaseSerializerTest {

    @Inject
    private Serializer<NarrativeSections> serializer;

    @Test
    public void readTest() throws IOException {
        final String testFile = "data/narrative_sections.csv";

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(testFile)) {
            NarrativeSections sections = serializer.read(in, errors);
            assertNotNull(sections);

            List<NarrativeScene> scenes = sections.asScenes();
            assertNotNull(scenes);
            // There is one line that is missing Lecoy
            assertEquals(65, scenes.size());

            NarrativeScene scene = scenes.get(0);
            assertEquals("G 1a", scene.getId());
            assertEquals(1, scene.getCriticalEditionStart());
            assertEquals(20, scene.getCriticalEditionEnd());
            assertEquals(1, scene.getRel_line_start());
            assertEquals(20, scene.getRel_line_end());
            assertEquals("Preface", scene.getDescription());
        }
    }

    @Test (expected = UnsupportedOperationException.class)
    public void writeTest() throws IOException {
        OutputStream out = mock(OutputStream.class);
        serializer.write(new NarrativeSections(), out);
    }

}
