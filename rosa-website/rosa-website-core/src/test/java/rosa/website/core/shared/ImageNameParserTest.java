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
                "front matter 002r", "front matter 002v", "020r", "020v", "A001r", "A002v"
        };

        for (int i = 0; i < names.length; i++) {
            assertEquals("Unexpected name found.", expected[i], ImageNameParser.toStandardName(names[i]));
        }
    }

    @Test
    public void shortNameToStandardShort() {
        String[] names = {
                "front matter 2r", "front matter 2v", "20r", "20v", "A1r", "A2v"
        };

        String[] expected = {
                "front matter 002r", "front matter 002v", "020r", "020v", "A001r", "A002v"
        };

        for (int i = 0; i < names.length; i++) {
            assertEquals("Unexpected name found.", expected[i], ImageNameParser.toStandardName(names[i]));
        }
    }
}
