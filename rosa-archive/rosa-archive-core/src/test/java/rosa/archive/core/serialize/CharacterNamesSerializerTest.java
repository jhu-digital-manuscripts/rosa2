package rosa.archive.core.serialize;

import org.junit.Before;
import org.junit.Test;
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
public class CharacterNamesSerializerTest {
    private static final String testFile = "character_names.csv";

    private CharacterNamesSerializer serializer;

    @Before
    public void setup() {
        this.serializer = new CharacterNamesSerializer();
    }

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
