package rosa.archive.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Test handling of cropped images.
 */
public class StoreCropTest extends BaseStoreTest {

    /**
     * Should not crop if not forced and the cropped directory already exists.
     *
     * @throws IOException
     */
    @Test
    public void testDoNotCropExistingImages() throws IOException {
        // Create cropped directory
        ByteStreamGroup bookGroup = new FSByteStreamGroup(testBookPath);
        assertNotNull(bookGroup);
        bookGroup.newByteStreamGroup("cropped");
        assertTrue(bookGroup.hasByteStreamGroup("cropped"));

        List<String> errors = new ArrayList<>();
        testStore.cropImages(COLLECTION_NAME, BOOK_NAME, false, errors);

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("images already exist for this book"));
    }

    /**
     * cropped/ directory does not exist, but the file *.images.crop.csv does exist.
     *
     * The code should execute and result in 1 error and all 21 images being
     * cropped and put to the cropped/ directory. The error will state that for one
     * image, no cropping information will exist. This image will be directly
     * copied to the cropped/ directory.
     *
     * @throws IOException
     */
    @Test
    public void forceImageCropDespiteCropFile() throws IOException {
        // Make sure cropped/ directory does not exist
        ByteStreamGroup bookGroup = new FSByteStreamGroup(testBookPath);
        assertNotNull(bookGroup);
        assertTrue(bookGroup.hasByteStream("LudwigXV7.images.csv"));
        assertTrue(bookGroup.hasByteStream("LudwigXV7.images.crop.csv"));
        assertFalse(bookGroup.hasByteStreamGroup("cropped"));

        // count the number of original images
        assertEquals(21, countImages(bookGroup));

        List<String> errors = new ArrayList<>();
        testStore.cropImages(COLLECTION_NAME, BOOK_NAME, true, errors);

        assertEquals(1, errors.size());
        assertEquals("Image missing from cropping information, copying old file. [LudwigXV7.frontmatter.flyleaf.001r.tif]",
                errors.get(0));
        assertTrue(bookGroup.hasByteStreamGroup("cropped"));
        assertEquals(21, countImages(bookGroup.getByteStreamGroup("cropped")));
    }

    /**
     * Count the number of TIFF images in a byte stream group
     *
     * @param bsg byte stream group
     * @return number
     * @throws IOException
     */
    private int countImages(ByteStreamGroup bsg) throws IOException {
        int count = 0;

        for (String name : bsg.listByteStreamNames()) {
            if (name.endsWith(ArchiveConstants.TIF_EXT)) {
                count++;
            }
        }

        return count;
    }

}
