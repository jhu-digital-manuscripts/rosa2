package rosa.archive.core.check;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.core.BaseGuiceTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
* @see rosa.archive.core.check.BookChecker
*/
public class BookCheckerTest extends BaseGuiceTest {
    // Test using the BookChecker in the base class (BaseGuiceTest)

    private List<String> errors;
    private List<String> warnings;

    @Before
    public void setup() {
        errors = new ArrayList<>();
        warnings = new ArrayList<>();
    }

    /**
     * Using the pre-loaded test collection + book, run the checker in the test store
     * with checksums disabled. The check should pass with no errors or warnings.
     */
    @Test
    public void loadLudwigAndCheckWithoutBits() throws Exception {
        // Use testCollection and testBook from the base class
        boolean check = store.check(loadValidCollection(), loadValidLudwigXV7(), false, errors, warnings);
        // testBook data contains references to many images that do not exist in the test directory.
        // these will all appear as errors!
        assertTrue(check);
        assertTrue("There should be NO errors.", errors.isEmpty());
        assertTrue("Warnings list should be empty.", warnings.isEmpty());
    }

    /**
     * Using the pre-loaded test collection + book, run the checker in the test store
     * with checksums enabled. The check should pass with no errors or warnings. All
     * checksums should validate.
     */
    @Test
    public void loadLudwigAndCheckWithBits() throws Exception {
        // Use testCollection and testBook from the base class
        boolean check = store.check(loadValidCollection(), loadValidLudwigXV7(), true, errors, warnings);
        assertTrue(check);
        assertTrue("There should be NO errors.", errors.isEmpty());
        assertTrue("Warnings list should be empty.", warnings.isEmpty());
    }

    /**
     * Load FolgersHa2, which contains AoR transcription data, and run the checker.
     * The check should pass with no errors or warnings. Checksums should validate.
     */
    @Test
    public void loadFolgersAndCheckWithBits() throws Exception {
//        fail("Test not implemented.");
    }

}
