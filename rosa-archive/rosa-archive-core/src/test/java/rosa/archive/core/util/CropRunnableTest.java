package rosa.archive.core.util;

import org.junit.Before;
import org.junit.Test;

import rosa.archive.core.BaseArchiveTest;
import rosa.archive.model.BookImage;
import rosa.archive.model.CropData;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class CropRunnableTest extends BaseArchiveTest {
    private static final String IMAGE_ID = "FolgersHa2.001r.tif";

    private CropData crop;
    private BookImage image;

    private Path bookPath;
    private List<String> errors;

    @Before
    public void setup() {
        image = new BookImage();
        image.setId(IMAGE_ID);
        image.setWidth(3);
        image.setHeight(3);

        crop = new CropData();
        crop.setId(IMAGE_ID);
        crop.setTop(0.1);
        crop.setBottom(0.1);
        crop.setLeft(0.1);
        crop.setRight(0.1);

        bookPath = basePath.resolve(VALID_COLLECTION).resolve(VALID_BOOK_FOLGERSHA2);
        errors = new ArrayList<>();


    }

    /**
     * Input data for the CropRunnable includes initial width and height and the crop percent
     * for top, bottom, left, and right. Make sure the top, bottom, left, and right position
     * of the crop are calculated correctly in pixels.
     */
    @Test
    public void fourCornersTest() {
        image = new BookImage(IMAGE_ID, 1000, 1000, false);
        CropRunnable runnable = new CropRunnable(bookPath.toString(), image, crop, "cropped", errors);

        int[] corners = runnable.calcPoints();

        assertNotNull(corners);
        assertEquals(4, corners.length);
        assertEquals(100, corners[0]);
        assertEquals(900, corners[1]);
        assertEquals(100, corners[2]);
        assertEquals(900, corners[3]);
    }

    @Test
    public void cropTest() {
        CropRunnable runnable = new CropRunnable(bookPath.toString(), image, crop, "cropped", errors);

        Path expectedCropPath = bookPath.resolve("cropped").resolve(IMAGE_ID);

        assertTrue("Source image doesn't exist.", Files.exists(bookPath.resolve(IMAGE_ID)));
        assertFalse("Target image should NOT already exist.", Files.exists(expectedCropPath));

        runnable.run();

        assertTrue("Errors were found while cropping image.", errors.isEmpty());
        assertTrue("Cropped image not found.", Files.exists(expectedCropPath));
    }

}
