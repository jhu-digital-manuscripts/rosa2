package rosa.archive.core.serialize;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import rosa.archive.core.ArchiveCoreModule;
import rosa.archive.core.GuiceJUnitRunner;
import rosa.archive.core.GuiceJUnitRunner.GuiceModules;
import rosa.archive.model.CharacterNames;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @see rosa.archive.core.serialize.CharacterNamesSerializer
 */
@RunWith(GuiceJUnitRunner.class)
@GuiceModules({ArchiveCoreModule.class})
public class CharacterNamesSerializerTest {
    private static final String testFile = "data/character_names.csv";

    @Inject
    private Serializer<CharacterNames> serializer;

    @Test
    public void readsFromFile() throws IOException {

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(testFile)) {

            CharacterNames names = serializer.read(in);
            assertNotNull(names);

            assertEquals(78, names.getAllCharacterIds().size());
            assertEquals(77, names.getAllNamesInLanguage("English name").size());
            assertEquals(77, names.getAllNamesInLanguage("Site name").size());
            assertTrue(names.getAllNamesInLanguage("French variant").size() > 0);
            assertTrue(names.getAllNamesInLanguage("French variant").size() < 78);

            assertEquals("Diogenes", names.getNameInLanguage("36", "Site name"));
            assertEquals("Dyogenes, Dyogenés", names.getNameInLanguage("36", "French variant"));
            assertEquals("Diogenes", names.getNameInLanguage("36", "English name"));

        }

    }

    @Test (expected = UnsupportedOperationException.class)
    public void writeTest() throws IOException {
        OutputStream out = mock(OutputStream.class);
        serializer.write(new CharacterNames(), out);
    }

}
