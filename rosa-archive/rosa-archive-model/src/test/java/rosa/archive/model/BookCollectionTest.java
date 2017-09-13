package rosa.archive.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * @see BookCollection
 */
public class BookCollectionTest {
    private static final String[] LANGS = { "en", "fr", "de" };

    private BookCollection collection;

    @Before
    public void setup() {
        this.collection = new BookCollection();
        CollectionMetadata cmd = new CollectionMetadata();
        this.collection.setMetadata(cmd);

        // Add supported languages
        cmd.setLanguages(LANGS);
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
