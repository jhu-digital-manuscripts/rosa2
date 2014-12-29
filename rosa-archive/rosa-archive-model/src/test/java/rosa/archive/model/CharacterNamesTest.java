package rosa.archive.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class CharacterNamesTest {
    private static final int MAX_CHARS = 10;
    private static final String[] NAMES = { "Scott Pilgrim", "Marie Jeanne", "Johann Bach" };
    private static final String[] LANGS = { "en", "fr", "de" };

    private CharacterNames names;

    @Before
    public void setup() {
        this.names = new CharacterNames();

        for (int i = 0; i < MAX_CHARS; i++) {
            CharacterName name = mock(CharacterName.class);
            when(name.getId()).thenReturn(String.valueOf(i));
            when(name.getAllNames()).thenReturn( new HashSet<>( Arrays.asList(NAMES) ));

            for (int j = 0; j < 3; j++) {
                when(name.getNameInLanguage(LANGS[j])).thenReturn(NAMES[j]);
            }
            names.addCharacterName(name);
        }
    }

    @Test
    public void getAllCharacterIdsReturnsAllIds() {
        Set<String> ids = names.getAllCharacterIds();

        assertNotNull(ids);
        assertEquals(MAX_CHARS, ids.size());
        for (int i = 0; i < MAX_CHARS; i++) {
            assertTrue(ids.contains(String.valueOf(i)));
        }
    }

    @Test
    public void getAllNamesInLanguageWorks() {
        // Individually check english, french, and german names
        for (int i = 0; i < 3; i++) {
            String lang = LANGS[i];
            Set<String> namesForLang = names.getAllNamesInLanguage(lang);

            assertNotNull(namesForLang);
            assertTrue(namesForLang.size() > 0);
            for (String name : namesForLang) {
                assertEquals(NAMES[i], name);
            }
        }
    }

    @Test
    public void getNameInLanguageGetsWorks() {
        for (int i = 0; i < MAX_CHARS; i++) {
            assertEquals(NAMES[0], names.getNameInLanguage(String.valueOf(i), LANGS[0]));
            assertEquals(NAMES[1], names.getNameInLanguage(String.valueOf(i), LANGS[1]));
            assertEquals(NAMES[2], names.getNameInLanguage(String.valueOf(i), LANGS[2]));
        }
    }

}
