package rosa.archive.tool;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.core.BaseArchiveTest;
import rosa.archive.core.serialize.ImageListSerializer;
import rosa.archive.model.BookImage;
import rosa.archive.model.ImageList;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ImageListDecoratorTest extends BaseArchiveTest {

    private ImageListDecorator decorator;

    @Before
    public void setup() {
        decorator = new ImageListDecorator(store, base, new PrintStream(new ByteArrayOutputStream()));
    }

    /**
     * Make sure that the tool adds page labels to the image list.
     */
    @Test
    public void itDoesWhatItsSupposedToTest() throws Exception {
        ImageList before = loadValidFolgersHa2().getImages();
        assertNotNull(before);

        decorator.run(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2);

        ImageList after;
        try (InputStream in = base.getByteStreamGroup(VALID_COLLECTION).getByteStreamGroup(VALID_BOOK_FOLGERSHA2)
                .getByteStream("FolgersHa2.images.csv")) {
            List<String> errors = new ArrayList<>();
            after = new ImageListSerializer().read(in, errors);

            assertTrue(errors.isEmpty());
        }
        assertNotNull(after);

        assertEquals("Image list before and after must have the same number of images",
                before.getImages().size(), after.getImages().size());
        assertNotEquals(before, after);

        // Make sure that ALL image IDs found in 'before' are also found in 'after'
        before.getImages().stream().map(BookImage::getId).forEach(imageId -> assertTrue(containsImageId(imageId, after)));
        assertTrue(after.getImages().stream().map(BookImage::getName).count() > 0);

        assertTrue(after.getImages().stream().anyMatch(image -> image.getName().equals("322")));
    }

    private boolean containsImageId(String id, ImageList images) {
        return images.getImages().stream().anyMatch(image -> image.getId().equals(id));
    }

}
