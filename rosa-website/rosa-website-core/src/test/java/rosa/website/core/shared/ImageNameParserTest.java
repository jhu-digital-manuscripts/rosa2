package rosa.website.core.shared;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ImageNameParserTest {

    @Test
    public void standardNameTest() {
        String[] names = {
                "LudwigXV7.frontmatter.flyleaf.002r.tif", "LudwigXV7.frontmatter.flyleaf.002v.tif",
                "LudwigXV7.020r.tif", "LudwigXV7.020v.tif", "LudwigXV7.A1r.tif", "LudwigXV7.A2v.tif"
        };
        String[] expected = {
                "frontmatter 002r", "frontmatter 002v", "020r", "020v", "A001r", "A002v"
        };

        for (int i = 0; i < names.length; i++) {
            assertEquals("Unexpected name found.", expected[i], ImageNameParser.toStandardName(names[i]));
        }
    }
}
