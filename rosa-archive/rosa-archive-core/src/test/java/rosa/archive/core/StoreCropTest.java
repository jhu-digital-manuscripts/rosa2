package rosa.archive.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

/**
 * Test handling of cropped images.
 */
public class StoreCropTest extends BaseTmpStoreTest {
    private Path getCroppedDir(String collection, String book) {
        return getTmpBookPath(collection, book).resolve("cropped");
    }

    /**
     * Should not crop if not forced and the cropped directory already exists.
     * 
     * @throws IOException
     */
    @Test
    public void testDoNotCropExistingImages() throws IOException {
        Files.createDirectory(getCroppedDir(VALID_COLLECTION_NAME, VALID_BOOK_LUDWIGXV7));

        List<String> errors = new ArrayList<>();
        tmpStore.cropImages(VALID_COLLECTION_NAME, VALID_BOOK_LUDWIGXV7, false, errors);

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("images already exist for this book"));
    }

    /**
     * cropped/ directory does not exist, but the file *.images.crop.csv does
     * exist.
     * 
     * The code should execute and result in 1 error and all 21 images being
     * cropped and put to the cropped/ directory. The error will state that for
     * one image, no cropping information will exist. This image will be
     * directly copied to the cropped/ directory.
     * 
     * @throws IOException
     */
    @Test
    public void forceImageCropDespiteCropFile() throws IOException {
        // Make sure cropped/ directory does not exist

        Path crop_dir = getCroppedDir(VALID_COLLECTION_NAME, VALID_BOOK_LUDWIGXV7);
        assertFalse(Files.exists(crop_dir));

        // count the number of original images
        assertEquals(287, countImages(getTmpBookPath(VALID_COLLECTION_NAME, VALID_BOOK_LUDWIGXV7)));
        
        List<String> errors = new ArrayList<>();
        tmpStore.cropImages(VALID_COLLECTION_NAME, VALID_BOOK_LUDWIGXV7, true, errors);

        assertEquals(0, errors.size());
        assertTrue(Files.exists(crop_dir));
        assertEquals(287, countImages(crop_dir));
    }

    /**
     * Count the number of TIFF images in a directory
     */
    private int countImages(Path dir) throws IOException {
        try (DirectoryStream<Path> images = Files.newDirectoryStream(dir, "*" + ArchiveConstants.TIF_EXT)) {
            int count = 0;

            for (Iterator<Path> iter = images.iterator(); iter.hasNext(); iter.next(), count++)
                ;

            return count;
        }
    }

}
