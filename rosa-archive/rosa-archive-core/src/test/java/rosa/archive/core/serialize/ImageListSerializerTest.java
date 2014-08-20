package rosa.archive.core.serialize;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import rosa.archive.core.ArchiveCoreModule;
import rosa.archive.core.GuiceJUnitRunner;
import rosa.archive.core.GuiceJUnitRunner.GuiceModules;
import rosa.archive.model.BookImage;
import rosa.archive.model.ImageList;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @see rosa.archive.core.serialize.ImageListSerializer
 */
@RunWith(GuiceJUnitRunner.class)
@GuiceModules({ArchiveCoreModule.class})
public class ImageListSerializerTest extends BaseSerializerTest{

    @Inject
    private Serializer<ImageList> serializer;

    @Test
    public void readTest() throws IOException {
        final String testFile = "data/Walters143/Walters143.images.csv";

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(testFile)) {

            ImageList images = serializer.read(in, errors);
            assertNotNull(images);

            List<BookImage> imgList = images.getImages();
            assertNotNull(imgList);
            assertEquals(81, imgList.size());

            // Missing image
            BookImage missingImage = imgList.get(0);
            assertNotNull(missingImage);
            assertEquals("Walters143.binding.frontcover.tif", missingImage.getId());
            assertEquals(0, missingImage.getWidth());
            assertEquals(0, missingImage.getHeight());
            assertTrue(missingImage.isMissing());

            // Non-missing image
            BookImage image = imgList.get(80);
            assertNotNull(image);
            assertEquals("Walters143.039r.tif", image.getId());
            assertEquals(2076, image.getWidth());
            assertEquals(2860, image.getHeight());
            assertFalse(image.isMissing());
        }
    }

    @Test (expected = UnsupportedOperationException.class)
    public void writeTest() throws IOException {
        OutputStream out = mock(OutputStream.class);
        serializer.write(new ImageList(), out);
    }

}
