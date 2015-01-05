package rosa.archive.core.check;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.core.BaseStoreTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
* @see rosa.archive.core.check.BookChecker
*/
public class BookCheckerTest extends BaseStoreTest {
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
     * with checksums disabled. This will result in 2495 errors, each of which are
     * the result of missing images. No warnings should be issued.
     */
    @Test
    public void loadLudwigAndCheckWithoutBits() throws Exception {
        // Use testCollection and testBook from the base class
        boolean check = testStore.check(testCollection, testBook, false, errors, warnings);
        // testBook data contains references to many images that do not exist in the test directory.
        // these will all appear as errors!
        assertFalse(check);
        assertEquals("Unexpected number of errors found.", 2495, errors.size());

        // Check the number of errors that are associated with missing images
        // (This comes from following page references to page images)
        int imageAssociations = 0;
        for (String error : errors) {
            if (error.contains("start page") || error.contains("end page")
                    || error.toLowerCase().contains("image")
                    || error.contains("Cropping information")){
                imageAssociations++;
            }
        }
        assertEquals("Error(s) found that were not associated with missing images.", imageAssociations, errors.size());
        assertTrue("Warnings list should be empty.", warnings.isEmpty());
    }

    /**
     * Using the pre-loaded test collection + book, run the checker in the test store
     * with checksums enabled. This will result in 2495 errors, each of which are
     * the result of missing images. No warnings should be issued. All checksums
     * should validate.
     */
    @Test
    public void loadLudwigAndCheckWithBits() throws Exception {
        // Use testCollection and testBook from the base class
        boolean check = testStore.check(testCollection, testBook, true, errors, warnings);
        assertFalse(check);
        assertEquals("Unexpected number of errors found.", 2495, errors.size());

        int imageAssociations = 0;
        for (String error : errors) {
            if (error.contains("start page") || error.contains("end page")
                    || error.toLowerCase().contains("image")
                    || error.contains("Cropping information")){
                imageAssociations++;
            }
        }
        assertEquals("Error(s) found that were not associated with missing images.", imageAssociations, errors.size());
        assertTrue("Warnings list should be empty.", warnings.isEmpty());
    }

    // TODO run checker with a book that has AoR data

}
