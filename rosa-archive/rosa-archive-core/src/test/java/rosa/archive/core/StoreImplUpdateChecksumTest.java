package rosa.archive.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import rosa.archive.core.serialize.SHA1ChecksumSerializer;
import rosa.archive.model.SHA1Checksum;

/**
 * Check creating and updating checksums.
 */
public class StoreImplUpdateChecksumTest extends BaseTmpStoreImplTest {

    /**
     * Check creation of new checksums for a collection.
     */
    @Test
    public void createNewChecksumsForCollection() throws Exception {
        // Grab existing checksums
        SHA1Checksum expected = loadTmpCollection(VALID_COLLECTION).getChecksum();

        assertNotNull(expected);
        assertFalse(expected.checksums().isEmpty());

        // Delete checksums file
        removeCollectionFile(VALID_COLLECTION, expected.getId());

        // Force update of the checksum and test
        List<String> errors = new ArrayList<>();
        boolean result = tmpStore.updateChecksum(VALID_COLLECTION, true, errors);

        assertTrue(result);
        assertEquals(0, errors.size());
        assertEquals(expected, loadTmpCollection(VALID_COLLECTION).getChecksum());
    }

    /**
     * Test creating new checksums for a book.
     */
    @Test
    public void createNewChecksumsForBook() throws Exception {
        List<String> errors = new ArrayList<>();

        // Grab existing checksums
        SHA1Checksum expected = loadTmpBook(VALID_COLLECTION, VALID_BOOK_LUDWIGXV7).getChecksum();
        assertNotNull(expected);
        assertFalse(expected.checksums().isEmpty());

        assertNotNull(expected.getId());

        // Delete checksums file
        removeBookFile(VALID_COLLECTION, VALID_BOOK_LUDWIGXV7, expected.getId());

        // Force update of the checksum and test
        boolean result = tmpStore.updateChecksum(VALID_COLLECTION, VALID_BOOK_LUDWIGXV7, true, errors);
        assertTrue(result);
        assertEquals(0, errors.size());

        SHA1Checksum test = loadTmpBook(VALID_COLLECTION, VALID_BOOK_LUDWIGXV7).getChecksum();
        assertEquals(0, errors.size());

        assertEquals(expected.getAllIds().size(), test.getAllIds().size());
        for (String id: expected.getAllIds()) {
            
             assertEquals(id, expected.checksums().get(id), test.checksums().get(id));
        }
        
        assertEquals(expected, test);
    }

    @Test
    public void testUpdateBadChecksums() throws Exception {
        List<String> errors = new ArrayList<>();

        SHA1Checksum expected = loadValidLudwigXV7().getChecksum();
        assertNotNull(expected);
        assertTrue(expected.checksums().size() > 0);

        // Add an incorrect entry and write it out
        SHA1Checksum wrong = new SHA1Checksum();
        wrong.checksums().putAll(expected.checksums());

        String wrong_entry = wrong.checksums().keySet().iterator().next();
        wrong.checksums().put(wrong_entry, "wrong");

        SHA1ChecksumSerializer s = new SHA1ChecksumSerializer();

        Path cs_path = getTmpBookPath(VALID_COLLECTION, VALID_BOOK_LUDWIGXV7).resolve(expected.getId());
        
        try (OutputStream os = Files.newOutputStream(cs_path)) {
            s.write(wrong, os);
        }

        assertTrue(tmpStore.updateChecksum(VALID_COLLECTION, VALID_BOOK_LUDWIGXV7, false, errors));
        assertEquals(0, errors.size());

        SHA1Checksum test = loadValidLudwigXV7().getChecksum();
        
        assertEquals(0, errors.size());
        assertEquals(expected.checksums().get(wrong_entry), test.checksums().get(wrong_entry));
        assertEquals(expected, test);
    }
}