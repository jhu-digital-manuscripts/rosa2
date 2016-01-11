package rosa.archive.core;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.ArchiveItemType;
import rosa.archive.model.BookCollection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StoreImplRenameTranscriptionsTest extends BaseArchiveTest {
    private static final ArchiveNameParser parser = new ArchiveNameParser();

    private List<String> errors;

    /**
     * Set up. Create fresh list to hold errors.
     */
    @Before
    public void setup() {
        errors = new ArrayList<>();
    }

    /**
     * Rename all AoR transcriptions according to a file map.
     *
     * @throws IOException
     */
    @Test
    public void renameTranscriptionsNormalTest() throws IOException {
        generateFileMap(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2, "BOOK_ID", true, true, 10, 10, 1);

        store.renameTranscriptions(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2, false, errors);
        assertTrue("Unexpected errors found.", errors.isEmpty());

        List<String> names = base.getByteStreamGroup(VALID_COLLECTION).getByteStreamGroup(VALID_BOOK_FOLGERSHA2)
                .listByteStreamNames();
        checkPages("BOOK_ID", names);

        assertTrue(names.contains("BOOK_ID.aor.frontmatter.flyleaf.001v.xml"));
        assertTrue(names.contains("BOOK_ID.aor.binding.frontcover.xml"));
        assertTrue(names.contains("BOOK_ID.aor.032v.xml"));
        checkWithBookChecker(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2);
    }

    /**
     * Rename all AoR transcriptions according to a file map, then successfully
     * rename all AoR transcriptions back to their original names.
     *
     * @throws IOException .
     */
//    @Test
//    public void renameTranscriptionsReverseTest() throws IOException {
//        generateFileMap(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2, "BOOK_ID", true, true, 10, 10, 1);
//
//        store.renameTranscriptions(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2, false, errors);
//        assertTrue("Unexpected errors found.", errors.isEmpty());
//
//        store.renameTranscriptions(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2, true, errors);
//        assertTrue("Unexpected errors found.", errors.isEmpty());
//
//        List<String> names = base.getByteStreamGroup(VALID_COLLECTION).getByteStreamGroup(VALID_BOOK_FOLGERSHA2)
//                .listByteStreamNames();
//        checkPages("FolgersHa2", names);
//
//        assertTrue(names.contains("FolgersHa2.037v.xml"));
//        assertFalse(names.contains("FolgersHa2.aor.frontmatter.flyleaf.001v.xml"));
//        checkWithBookChecker(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2);
//    }

    /**
     * Does not rename any items due to duplicate values in the file map.
     *
     * @throws IOException
     */
    @Test
    public void dontRenameWithBadFileMap() throws IOException {
        generateFileMap(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2, "BOOK_ID", true, true, 10, 10, 5);

        store.renameTranscriptions(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2, false, errors);
        assertFalse("Errors should occur, but were not encountered.", errors.isEmpty());
        checkWithBookChecker(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2);
    }

    private boolean checkWithBookChecker(String collection, String book) throws IOException {
        BookCollection col = store.loadBookCollection(collection, errors);
        return store.check(col, store.loadBook(col, book, errors), false, errors, null);
    }

    private void checkPages(String expectedId, List<String> names) {
        for (String name : names) {
            if (parser.getArchiveItemType(name) == ArchiveItemType.TRANSCRIPTION_AOR) {
                assertTrue("Unexpected ID found in image name.", name.startsWith(expectedId));
            }
        }
    }

    private void generateFileMap(String collection, String book, String id, boolean hasFront, boolean hasBack,
                                 int front, int end, int misc) throws IOException {
        ByteStreamGroup bookStreams = base.getByteStreamGroup(VALID_COLLECTION).
                getByteStreamGroup(VALID_BOOK_FOLGERSHA2);
        if (!bookStreams.hasByteStream("filemap.csv")) {
            // Create a file map, because the test data does not include it
            store.generateFileMap(collection, book, id, hasFront, hasBack, front, end, misc, errors);

            assertTrue("Unexpected errors found.", errors.isEmpty());
            assertTrue("Failed to create file map.", bookStreams.hasByteStream("filemap.csv"));
        }
    }

}
