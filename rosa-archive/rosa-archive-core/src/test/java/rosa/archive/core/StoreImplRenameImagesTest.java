package rosa.archive.core;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.ArchiveItemType;
import rosa.archive.model.BookImageLocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StoreImplRenameImagesTest extends BaseArchiveTest {

    private static final ArchiveNameParser parser = new ArchiveNameParser();

    private List<String> errors;

    @Before
    public void setup() throws IOException {
        errors = new ArrayList<>();
    }

    /**
     * Successfully rename all images in FolgersHa2.
     *
     * @throws IOException
     */
    @Test
    public void renameImagesTest() throws IOException {
        generateFileMap(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2, "BOOK_ID", true, true, 10, 10, 1);

        store.renameImages(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2, false, false, errors);
        assertTrue("Unexpected errors found.", errors.isEmpty());

        ByteStreamGroup bookGroup = base.getByteStreamGroup(VALID_COLLECTION).getByteStreamGroup(VALID_BOOK_FOLGERSHA2);
        int[] counts = countPages(bookGroup.listByteStreamNames());
        assertEquals("Unexpected number of front matter pages found.", 11, counts[0]);
        assertEquals("Unexpected number of end matter pages found.", 11, counts[2]);
        assertEquals("Unexpected number of misc pages found.", 1, counts[3]);

        checkImageIds("BOOK_ID", bookGroup.listByteStreamNames());
    }

    /**
     * Successfully rename all images in FolgersHa2, then rename them back
     * to their original names using the REVERSE flag.
     *
     * @throws IOException
     */
    @Test
    public void renameImagesReverseTest() throws IOException {
        generateFileMap(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2, "BOOK_ID", true, true, 10, 10, 1);

        store.renameImages(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2, false, false, errors);
        assertTrue("Unexpected errors found.", errors.isEmpty());

        store.renameImages(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2, false, true, errors);
        assertTrue("Unexpected errors found.", errors.isEmpty());

        List<String> names =
                base.getByteStreamGroup(VALID_COLLECTION).getByteStreamGroup(VALID_BOOK_FOLGERSHA2).listByteStreamNames();
        checkImageIds("FolgersHa2", names);
        int[] count = countPages(names);

        assertEquals("Unexpected number of front matter pages found.", 7, count[0]);
        assertEquals("Unexpected number of end matter pages found.", 7, count[2]);
        assertEquals("Unexpected number of misc pages found.", 3, count[3]);

        assertTrue(names.contains("FolgersHa2.frontmatter.flyleaf.003r.tif"));
        assertTrue(names.contains("FolgersHa2.endmatter.flyleaf.002v.tif"));
        assertTrue(names.contains("FolgersHa2.211v.tif"));
        assertTrue("", names.contains("FolgersHa2.misc.foreedge.tif"));
        assertTrue("", names.contains("FolgersHa2.misc.tail.tif"));
    }

    /**
     * Rename zero images if the file map contains multiple target names
     * with the same value. If the operation would succeed, then it would result in
     * multiple images being written to the same file name, ending with data loss.
     *
     * @throws IOException
     */
    @Test
    public void doesNotRenameWithBadFileMap() throws IOException {
        generateFileMap(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2, "BOOK_ID", true, true, 10, 10, 5);
        store.renameImages(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2, false, false, errors);
        assertFalse("Errors should have occurred.", errors.isEmpty());

        checkImageIds("FolgersHa2", base.getByteStreamGroup(VALID_COLLECTION).getByteStreamGroup(VALID_BOOK_FOLGERSHA2)
                .listByteStreamNames());
    }

    /**
     * All images are given a bad book ID in its name. They are then renamed to have
     * the correct book ID in its name.
     *
     * @throws IOException
     */
    @Test
    public void renameByChangingIdsOnly() throws IOException {
        ByteStreamGroup bookGroups = base.getByteStreamGroup(VALID_COLLECTION).getByteStreamGroup(VALID_BOOK_FOLGERSHA2);
        int[] initial_count = countPages(bookGroups.listByteStreamNames());

        // Rename images
        for (String name : bookGroups.listByteStreamNames()) {
            if (parser.getArchiveItemType(name) == ArchiveItemType.IMAGE) {
                String newName = "BadId" + name.substring(name.indexOf('.'));
                bookGroups.renameByteStream(name, newName);
            }
        }
        checkImageIds("BadId", bookGroups.listByteStreamNames());

        store.renameImages(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2, true, false, errors);
        int[] count = countPages(bookGroups.listByteStreamNames());

        for (int i = 0; i < count.length; i++) {
            assertEquals("Page count different.", initial_count[i], count[i]);
        }

        checkImageIds("FolgersHa2", bookGroups.listByteStreamNames());
    }

    /**
     * Make sure all image names start with the correct book ID.
     *
     * @param goodId book ID to check for
     * @param names list of file names
     */
    private void checkImageIds(String goodId, List<String> names) {
        for (String name : names) {
            if (parser.getArchiveItemType(name) == ArchiveItemType.IMAGE) {
                assertTrue("Bad ID found in image name.", name.startsWith(goodId));
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

    private int[] countPages(List<String> names) {
        int num_images = 0;
        int num_frontmatter = 0;
        int num_endmatter = 0;
        int num_misc = 0;
        int num_body = 0;

        for (String name : names) {
            if (parser.getArchiveItemType(name) == ArchiveItemType.IMAGE) {
                num_images++;

                BookImageLocation location = parser.location(name);
                if (location == null) {
                    continue;
                }
                switch (location) {
                    case FRONT_MATTER:
                        num_frontmatter++;
                        break;
                    case BODY_MATTER:
                        num_body++;
                        break;
                    case END_MATTER:
                        num_endmatter++;
                        break;
                    case MISC:
                        num_misc++;
                        break;
                    default:
                        break;
                }
            }
        }

        return new int[] {num_frontmatter, num_body, num_endmatter, num_misc, num_images};
    }

}
