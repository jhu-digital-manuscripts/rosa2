package rosa.archive.model;

import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CharacterNameTest {
    private static final String[] LANGS = { "en", "fr", "de" };
    private static final String[] NAMES = { "Scott Pilgrim", "Marie Jeanne", "Johann Bach" };

    private CharacterName name;

    @Before
    public void setup() {
        this.name = new CharacterName();

        name.setId("ThisIsAnId!");
        for (int i = 0; i < 3; i++) {
            name.addName(NAMES[i], LANGS[i]);
        }
    }

    @Test
    public void getNameInLanguageWorks() {
        for (int i = 0; i < 3; i++) {
            String lang = LANGS[i];
            String nameInLang = name.getNameInLanguage(lang);

            assertNotNull(nameInLang);
            assertEquals(NAMES[i], nameInLang);
        }
    }

    @Test
    public void getAllNamesReturnsAllNames() {
        Set<String> allNames = name.getAllNames();

        assertNotNull(allNames);
        for (String n : NAMES) {
            assertTrue(allNames.contains(n));
        }
    }

    @Test
    public void addNameWithoutLanguageDefaultsToEn() {
        String newName = "Lucas Lee";
        name.addName(newName);

        String nameInLang = name.getNameInLanguage(LANGS[0]);
        assertEquals(newName, nameInLang);

        Set<String> allNames = name.getAllNames();
        assertNotNull(allNames);
        assertTrue(allNames.contains(newName));
    }

}
