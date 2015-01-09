package rosa.archive.core.check;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import rosa.archive.core.BaseArchiveTest;
import rosa.archive.core.ByteStreamGroup;
import rosa.archive.model.BookCollection;

/**
 * @see rosa.archive.core.check.BookCollectionChecker
 */
public class BookCollectionCheckerTest extends BaseArchiveTest {

    private List<String> errors;
    private List<String> warnings;

    @Before
    public void setup() {
        errors = new ArrayList<>();
        warnings = new ArrayList<>();
    }

    /**
     * Collection makes logical sense. Checker succeeds with no error or warning messages.
     * Checksums are not checked, even if possible.
     */
    @Test
    public void checkValidCollectionSkippingBits() throws Exception {
        ByteStreamGroup validGroup = base.getByteStreamGroup(VALID_COLLECTION);
        assertNotNull("Byte Stream Group 'valid' should always exist.", validGroup);

        // Run check on a good collection
        assertTrue("Collection checker should succeed, but did not.",
                collectionChecker.checkContent(loadValidCollection(), validGroup, false, errors, warnings));
        assertTrue("Errors list should be empty.", errors.isEmpty());
        assertTrue("Warnings list should be empty.", warnings.isEmpty());
    }

    @Test
    public void checkValidCollectionWithBits() throws Exception {
        ByteStreamGroup validGroup = base.getByteStreamGroup(VALID_COLLECTION);
        assertNotNull("Byte Stream Group 'valid' should always exist.", validGroup);

        // Run check on good collection, checking the checksum data
        assertTrue("Collection checker should succeed.",
                collectionChecker.checkContent(loadValidCollection(), validGroup, true, errors, warnings));
        assertTrue("Errors list should be empty.", errors.isEmpty());
        assertTrue("Warnings list should be empty.", warnings.isEmpty());
    }

    /**
     * Collection makes logical sense except that it has no checksum reference. Checker fails
     * with exactly ONE error about the missing checksum. Checksums are not checked, even if possible.
     */
    @Test
    public void checkCollectionWithoutChecksumsSkippingBits() throws Exception {
        BookCollection collection = loadValidCollection();
        collection.setChecksum(null);

        ByteStreamGroup validGroup = base.getByteStreamGroup(VALID_COLLECTION);
        assertNotNull("Byte Stream Group 'valid' should always exist.", validGroup);

        assertFalse("Collection checker should fail.",
                collectionChecker.checkContent(collection, validGroup, false, errors, warnings));
        assertEquals("There should be only 1 error message.", 1, errors.size());
        assertEquals("Unexpected error message found.",
                "Checksum file is missing for collection. [valid]", errors.get(0));
        assertTrue("Warnings list should be empty.", warnings.isEmpty());
    }
}
