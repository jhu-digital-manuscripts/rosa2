package rosa.archive.core.serialize;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import rosa.archive.model.CharacterName;
import rosa.archive.model.CharacterNames;

/**
 * @see rosa.archive.core.serialize.CharacterNamesSerializer
 */
public class CharacterNamesSerializerTest extends BaseSerializerTest {
    private static final String testFile = "data/character_names.csv";

    private Serializer<CharacterNames> serializer;

    @Before
    public void setup() {
        super.setup();
        serializer = new CharacterNamesSerializer();
    }

    @Test
    @Override
    public void readTest() throws IOException {

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(testFile)) {

            CharacterNames names = serializer.read(in, errors);
            assertNotNull(names);

            assertEquals(117, names.getAllCharacterIds().size());
            assertEquals(116, names.getAllNamesInLanguage("en").size());
            assertEquals(116, names.getAllNamesInLanguage("Site name").size());
            assertTrue(names.getAllNamesInLanguage("fr").size() > 0);
            assertTrue(names.getAllNamesInLanguage("fr").size() < 117);

            assertEquals("Diogenes", names.getNameInLanguage("36", "Site name"));
            assertEquals("Dyogenes, Dyogenés", names.getNameInLanguage("36", "fr"));
            assertEquals("Diogenes", names.getNameInLanguage("36", "en"));

        }

    }

    @Test
    public void writeTest() throws IOException {
        CharacterNames names = createCharacterNames();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            serializer.write(names, out);
            String output = out.toString("UTF-8");

            assertNotNull(output);
            assertNotEquals("", output);

            List<String> lines = Arrays.asList(output.split("\n"));
            assertEquals(5, lines.size());

            assertTrue(lines.contains("ID,Site name,French variant,English name"));
            assertTrue(lines.contains("id1,name,name11,name1"));
            assertTrue(lines.contains("id2,name,name22,name2"));
            assertTrue(lines.contains("id3,name,name33,\"name3, n3, bleep\""));
            assertTrue(lines.contains("id4,,name44,n4"));
        }
    }

    private CharacterNames createCharacterNames() {
        CharacterNames names = new CharacterNames();

        CharacterName n1 = new CharacterName();
        n1.setId("id1");
        n1.addName("name", "site name");
        n1.addName("name1", "en");
        n1.addName("name11", "fr");
        names.addCharacterName(n1);

        CharacterName n2 = new CharacterName();
        n2.setId("id2");
        n2.addName("name", "site name");
        n2.addName("name2", "en");
        n2.addName("name22", "fr");
        names.addCharacterName(n2);

        CharacterName n3 = new CharacterName();
        n3.setId("id3");
        n3.addName("name", "site name");
        n3.addName("name3, n3, bleep", "en");
        n3.addName("name33", "fr");
        names.addCharacterName(n3);

        CharacterName n4 = new CharacterName();
        n4.setId("id4");
        n4.addName("name4", "en");
        n4.addName("n4", "en");
        n4.addName("name44", "fr");
        names.addCharacterName(n4);

        return names;
    }

}
