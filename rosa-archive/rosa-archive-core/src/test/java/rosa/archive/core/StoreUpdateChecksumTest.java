package rosa.archive.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import rosa.archive.core.serialize.SHA1ChecksumSerializer;
import rosa.archive.model.BookCollection;
import rosa.archive.model.SHA1Checksum;

/**
 * Check creating and updating checksums.
 */
@Ignore
public class StoreUpdateChecksumTest extends BaseStoreTest {
    
    /**
     * Check creation of new checksums for a collection.
     */
    @Test
    public void createNewChecksumsForCollection() throws Exception {
        List<String> errors = new ArrayList<>();

        // Grab existing checksums
        SHA1Checksum expected = testCollection.getChecksums();
        assertNotNull(expected);
        assertFalse(expected.checksums().isEmpty());

        // Delete checksums file
        removeCollectionFile(expected.getId());

        // Force update of the checksum and test
        boolean result = testStore.updateChecksum(COLLECTION_NAME, true, errors);
        assertTrue(result);
        assertEquals(0, errors.size());

        BookCollection col = testStore.loadBookCollection(COLLECTION_NAME, errors);
        assertEquals(0, errors.size());
        assertEquals(expected, col.getChecksums());
    }

    /**
     * Test creating new checksums for a book.
     */
    @Test
    public void createNewChecksumsForBook() throws Exception {
        List<String> errors = new ArrayList<>();

        // Grab existing checksums
        SHA1Checksum expected = testBook.getSHA1Checksum();
        assertNotNull(expected);
        assertFalse(expected.checksums().isEmpty());

        assertNotNull(expected.getId());
        
        // Delete checksums file
        removeBookFile(expected.getId());

        // Force update of the checksum and test
        boolean result = testStore.updateChecksum(COLLECTION_NAME, BOOK_NAME, true, errors);
        assertTrue(result);
        assertEquals(0, errors.size());

        SHA1Checksum test = testStore.loadBook(COLLECTION_NAME, BOOK_NAME, errors).getSHA1Checksum();
        assertEquals(0, errors.size());
        
        assertEquals(expected.getAllIds().size(), test.getAllIds().size());
        assertEquals(expected, test);
    }

    @Test
    public void testUpdateBadChecksums() throws Exception {
        List<String> errors = new ArrayList<>();

        SHA1Checksum expected = testBook.getSHA1Checksum();
        assertNotNull(expected);
        assertTrue(expected.checksums().size() > 0);
        
        // Add an incorrect entry and write it out
        SHA1Checksum wrong = new SHA1Checksum();
        wrong.checksums().putAll(expected.checksums());
        
        String wrong_entry = wrong.checksums().keySet().iterator().next(); 
        wrong.checksums().put(wrong_entry, "wrong");
        
        SHA1ChecksumSerializer s = new SHA1ChecksumSerializer();
        
        try (OutputStream os = Files.newOutputStream(testBookPath.resolve(expected.getId()))) {        
            s.write(wrong, os);
        }
                
        assertTrue(testStore.updateChecksum(COLLECTION_NAME, BOOK_NAME, false, errors));
        assertEquals(0, errors.size());

        SHA1Checksum test = testStore.loadBook(COLLECTION_NAME, BOOK_NAME, errors).getSHA1Checksum();
        assertEquals(0, errors.size());
        
        assertEquals(expected.checksums().get(wrong_entry), test.checksums().get(wrong_entry));
        assertEquals(expected, test);
    }
}