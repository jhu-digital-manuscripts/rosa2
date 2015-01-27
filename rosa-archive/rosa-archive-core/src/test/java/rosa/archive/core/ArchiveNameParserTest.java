package rosa.archive.core;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.ImageType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ArchiveNameParserTest {
    private final static String[] TEST_NAMES = {
            "LudwigXV7.binding.frontcover.tif", "LudwigXV7.frontmatter.flyleaf.002r.tif",
            "LudwigXV7.frontmatter.flyleaf.002v.tif", "LudwigXV7.020r.tif", "LudwigXV7.020v.tif",
            "LudwigXV7.endmatter.001v.tif", "LudwigXV7.misc.greyscale.tif", "filemap.csv"
    };

    private ArchiveNameParser parser;

    @Before
    public void setup() {
        parser = new ArchiveNameParser();
    }

    @Test
    public void categoryTest() {
        final ImageType[] expected = {
                ImageType.FRONTCOVER, ImageType.FRONTMATTER,
                ImageType.FRONTMATTER, ImageType.TEXT,
                ImageType.TEXT, ImageType.ENDMATTER,
                ImageType.MISC, ImageType.UNKNOWN
        };

        for (int i = 0; i < expected.length; i++) {
            ImageType type = parser.type(TEST_NAMES[i]);

            assertNotNull("No type found.", type);
            assertEquals("Unexpected type found.", expected[i], type);
        }
    }

    @Test
    public void pageTest() {
        final String[] expected = {
                null, "002r", "002v", "020r", "020v", "001v", null, null
        };

        for (int i = 0; i < expected.length; i++) {
            String page = parser.page(TEST_NAMES[i]);

            assertEquals("Unexpected page found.", expected[i], page);
        }
    }

    @Test
    public void bookIdTest() {
        final String[] expected = {
                "LudwigXV7", "LudwigXV7", "LudwigXV7", "LudwigXV7", "LudwigXV7", "LudwigXV7", "LudwigXV7", null
        };

        for (int i = 0; i < expected.length; i++) {
            String id = parser.bookId(TEST_NAMES[i]);

            assertEquals("Unexpected book ID found.", expected[i], id);
        }
    }

}
