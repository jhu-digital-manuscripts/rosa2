package rosa.archive.core.store;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.BookImage;
import rosa.archive.model.CropData;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 */
public class CropRunnableTest {

    private CropRunnable runnable;

    @Before
    public void setup() {
        BookImage image = new BookImage();
        image.setId("IMAGE");
        image.setWidth(1000);
        image.setHeight(1000);

        CropData crop = new CropData();
        crop.setId("IMAGE");
        crop.setTop(0.1);
        crop.setBottom(0.1);
        crop.setLeft(0.1);
        crop.setRight(0.1);

        List<String> errors = new ArrayList<>();
        runnable = new CropRunnable("/fake/", image, crop, "cropped", errors);
    }

    @Test
    public void fourCornersTest() {
        int[] corners = runnable.calcPoints();

        assertNotNull(corners);
        assertEquals(4, corners.length);
        assertEquals(100, corners[0]);
        assertEquals(900, corners[1]);
        assertEquals(100, corners[2]);
        assertEquals(900, corners[3]);
    }

    @Test
    public void buildCommandTest() {
        String expected = "convert /fake/IMAGE -crop 800x800+100+100 +repage /fake/cropped/IMAGE";
        assertEquals(expected, runnable.buildCommand());
    }

}
