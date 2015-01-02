package rosa.archive.core.check;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import rosa.archive.core.BaseGuiceTest;
import rosa.archive.core.ByteStreamGroup;
import rosa.archive.model.BookCollection;
import rosa.archive.model.CharacterName;
import rosa.archive.model.CharacterNames;
import rosa.archive.model.IllustrationTitles;
import rosa.archive.model.NarrativeScene;
import rosa.archive.model.NarrativeSections;
import rosa.archive.model.SHA1Checksum;

/**
 * @see rosa.archive.core.check.BookCollectionChecker
 */
public class BookCollectionCheckerTest extends BaseGuiceTest {
    private static final String[] bookNames = { "LudwigXV7", "Morgan948", "Senshu2", "Walters143" };

    private List<String> errors;
    private List<String> warnings;
    private ByteStreamGroup bsg;

    BookCollectionChecker checker;

    @Before
    public void setup() {
        errors = new ArrayList<>();
        warnings = new ArrayList<>();

        bsg = base.getByteStreamGroup("rosedata");

        checker = new BookCollectionChecker(serializers);
    }

    /**
     * Collection makes logical sense. Checker succeeds with no error or warning messages.
     * Checksums are not checked, even if possible.
     */
    @Test
    public void checkValidCollectionSkippingBits() throws Exception {
        // Run check on a good collection
        BookCollection collection = createBookCollection();
        assertTrue("Collection checker should succeed, but did not.",
                checker.checkContent(collection, bsg, false, errors, warnings));
        assertTrue("Errors list should be empty.", errors.isEmpty());
        assertTrue("Warnings list should be empty.", warnings.isEmpty());
    }

    /**
     * Collection makes logical sense except that it has no checksum reference. Checker fails
     * with exactly ONE error about the missing checksum. Checksums are not checked, even if possible.
     */
    @Test
    public void checkCollectionWithoutChecksumsSkippingBits() throws Exception {
        BookCollection collection = createBookCollection();
        collection.setChecksums(null);

        assertFalse("Collection checker should fail.", checker.checkContent(collection, bsg, false, errors, warnings));
        assertEquals("There should be only 1 error message.", 1, errors.size());
        assertEquals("Unexpected error message found.",
                "Checksum file is missing for collection. [rosedata]", errors.get(0));
        assertTrue("Warnings list should be empty.", warnings.isEmpty());
    }

    /**
     * Collection does not make logical sense, checker fails with errors, but no warnings.
     * Checksums are not checked, even if possible.
     */
    @Test
    public void checkInvalidCollectionSkippingBits() throws Exception {
        assertFalse("Collection checker should fail, but did not.",
                checker.checkContent(new BookCollection(), bsg, false, errors, warnings));
        assertFalse("Errors list should be empty.", errors.isEmpty());
        assertTrue("Warnings list should be empty.", warnings.isEmpty());
    }

    /**
     * Collection makes logical sense, checker passes with no errors/warnings. Checksums are
     * checked.
     */
    @Test
    public void checkValidCollectionWithBits() throws Exception {
        BookCollection collection = createBookCollection();
        assertTrue("Collection checker should pass, bud did not.",
                checker.checkContent(collection, bsg, true, errors, warnings));
        assertTrue("Errors list should be empty.", errors.isEmpty());
        assertTrue("Warnings list should be empty.", warnings.isEmpty());
    }

    /**
     * Collection makes logical sense except that it has no reference to checksums. Checker fails
     * with ONE error and no warnings. Checksums are checked if possible.
     */
    @Test
    public void checkCollectionWithoutChecksumWithBits() throws Exception {
        BookCollection collection = createBookCollection();
        collection.setChecksums(null);

        assertFalse("Collection checker should fail, but did not.",
                checker.checkContent(collection, bsg, true, errors, warnings));
        assertFalse("Errors list should not be empty.", errors.isEmpty());
        assertEquals("There should be exactly ONE error message.", 1, errors.size());
        assertEquals("Unexpected error message found.",
                "Checksum file is missing for collection. [rosedata]", errors.get(0));
        assertTrue("Warnings list should be empty.", warnings.isEmpty());
    }

    /**
     * Collection does not make logical sense. Checker fails with errors, but no warnings.
     * Checksums are checked if possible.
     */
    @Test
    public void checkInvalidCollectionWithBits() throws Exception {
        assertFalse("Collection checker should fail, but did not.",
                checker.checkContent(new BookCollection(), bsg, true, errors, warnings));
        assertFalse("Errors list should not be empty.", errors.isEmpty());
        assertTrue("Warnings list should be empty.", warnings.isEmpty());
    }

    /**
     * @return a BookCollection object with data that makes logical sense.
     */
    private BookCollection createBookCollection() {
        BookCollection collection = new BookCollection();
        collection.setId("rosedata");

        collection.setLanguages(new String[] { "en", "fr" });

        // Character names
        CharacterNames names = new CharacterNames();
        names.setId("character_names.csv");
        for (int i = 0; i < 5; i++) {
            CharacterName name = new CharacterName();
            name.setId("Character" + i);
            name.addName("Name" + i, "en");
            name.addName("Name" + i, "fr");

            names.addCharacterName(name);
        }
        collection.setCharacterNames(names);

        // Illustration titles
        IllustrationTitles titles = new IllustrationTitles();
        titles.setId("illustration_titles.csv");
        Map<String, String> titleMap = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            titleMap.put("Illustration" + i, "Illustration Title");
        }
        titles.setData(titleMap);
        collection.setIllustrationTitles(titles);

        // Narrative sections
        NarrativeSections sections = new NarrativeSections();
        sections.setId("narrative_sections.csv");
        List<NarrativeScene> scenes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            NarrativeScene scene = new NarrativeScene();
            scene.setId("Scene" + i);

            scenes.add(scene);
        }
        sections.setScenes(scenes);
        collection.setNarrativeSections(sections);

        collection.setBooks(bookNames);

        SHA1Checksum checksum = new SHA1Checksum();
        checksum.setId("SHA1SUM");
        collection.setChecksums(checksum);

        return collection;
    }
}
