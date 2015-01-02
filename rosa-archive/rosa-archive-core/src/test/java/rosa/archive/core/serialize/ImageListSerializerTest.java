package rosa.archive.core.serialize;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import rosa.archive.model.BookImage;
import rosa.archive.model.ImageList;

/**
 * @see rosa.archive.core.serialize.ImageListSerializer
 */
public class ImageListSerializerTest extends BaseSerializerTest{

    private Serializer<ImageList> serializer;

    @Before
    public void setup() {
        super.setup();
        serializer = new ImageListSerializer();
    }

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

    @Test
    public void writeTest() throws IOException {
        String[] names = {
                "LudwigXV7.001r.tif",
                "LudwigXV7.001v.tif",
                "LudwigXV7.binding.frontcover.tif",
                "LudwigXV7.frontmatter.pastedown.tif",
                "LudwigXV7.frontmatter.flyleaf.04v.tif",
                "LudwigXV7.frontmatter.flyleaf.04r.tif",
                "LudwigXV7.endmatter.flyleaf.01r.tif",
                "LudwigXV7.endmatter.flyleaf.02v.tif",
                "LudwigXV7.endmatter.pastedown.tif",
                "LudwigXV7.binding.backcover.tif"
        };

        ImageList list = new ImageList();

        List<BookImage> images = list.getImages();
        for (int i = 0; i < names.length; i++) {
            BookImage image = new BookImage();

            image.setId(names[i]);
            image.setWidth(100 + i);
            image.setHeight(200 + i);

            if (i % 3 == 0) {
                image.setMissing(true);
            } else {
                image.setMissing(false);
            }

            images.add(image);
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            serializer.write(list, out);

            String output = out.toString("UTF-8");
            assertNotNull(output);

            String[] lines = output.split("\\n");
            assertNotNull(lines);
            assertEquals(10, lines.length);
            assertTrue(lines[9].equals("*LudwigXV7.binding.backcover.tif,109,209"));
        }
    }

}
