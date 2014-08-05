package rosa.archive.model;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @see BookCollection
 */
public class BookCollectionTest {
    private static final String[] LANGS = { "en", "fr", "de" };

    private BookCollection collection;

    @Before
    public void setup() {
        this.collection = new BookCollection();

        // Add supported languages
        collection.setLanguages(LANGS);
    }

    @Test
    public void languageIsSupported() {
        assertTrue(collection.isLanguageSupported(LANGS[0]));
        assertTrue(collection.isLanguageSupported(LANGS[1]));
        assertTrue(collection.isLanguageSupported(LANGS[2]));
    }

    @Test
    public void languageNotSupported() {
        assertFalse(collection.isLanguageSupported("asdf"));
        assertFalse(collection.isLanguageSupported("is"));
    }

}
