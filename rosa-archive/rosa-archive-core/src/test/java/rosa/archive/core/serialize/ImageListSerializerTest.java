package rosa.archive.core.serialize;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import rosa.archive.core.ArchiveNameParser;
import rosa.archive.model.BookImage;
import rosa.archive.model.BookImageLocation;
import rosa.archive.model.BookImageRole;
import rosa.archive.model.ImageList;

/**
 * @see rosa.archive.core.serialize.ImageListSerializer
 */
public class ImageListSerializerTest extends BaseSerializerTest<ImageList> {

    private ArchiveNameParser parser;

    @Before
    public void setup() {
        serializer = new ImageListSerializer();
        parser = new ArchiveNameParser();
    }

    @Test
    public void readTest() throws IOException {
        ImageList images = loadResource(COLLECTION_NAME, BOOK_NAME, "LudwigXV7.images.csv");
        assertNotNull(images);

        List<BookImage> imgList = images.getImages();
        assertNotNull(imgList);
        assertEquals(288, imgList.size());

        // Missing image
        BookImage missingImage = imgList.get(0);
        assertNotNull(missingImage);
        assertEquals("LudwigXV7.binding.frontcover.tif", missingImage.getId());
        assertEquals(BookImageLocation.BINDING, missingImage.getLocation());
        assertEquals(BookImageRole.FRONT_COVER, missingImage.getRole());
        assertEquals(3, missingImage.getWidth());
        assertEquals(3, missingImage.getHeight());
        assertFalse(missingImage.isMissing());

        // Non-missing image
        BookImage image = imgList.get(80);
        assertNotNull(image);
        assertEquals("LudwigXV7.036r.tif", image.getId());
        assertEquals(BookImageLocation.BODY_MATTER, image.getLocation());
        assertEquals(3, image.getWidth());
        assertEquals(3, image.getHeight());
        assertFalse(image.isMissing());
    }

    @Test
    public void writeTest() throws IOException {
        List<String> lines = writeObjectAndGetWrittenLines(createImageList());

        assertNotNull(lines);
        assertEquals(10, lines.size());
        assertTrue(lines.contains("*LudwigXV7.binding.backcover.tif,109,209"));
    }

    private ImageList createImageList() {
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
            image.setName(parser.shortName(names[i]));
            image.setLocation(parser.location(names[i]));
            image.setRole(parser.role(names[i]));
            image.setWidth(100 + i);
            image.setHeight(200 + i);

            if (i % 3 == 0) {
                image.setMissing(true);
            } else {
                image.setMissing(false);
            }

            images.add(image);
        }

        return list;
    }

}
