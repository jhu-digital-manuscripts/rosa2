package rosa.archive.core.util;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.BookImage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class BookImageComparatorTest {

    private BookImageComparator comparator;

    @Before
    public void setup() {
        comparator = BookImageComparator.instance();
    }

    @Test
    public void comparatorTest() {
        String[] names = {
                "LudwigXV7.binding.frontcover.tif",
                "LudwigXV7.frontmatter.pastedown.tif",
                "LudwigXV7.frontmatter.flyleaf.04r.tif",
                "LudwigXV7.frontmatter.flyleaf.04v.tif",
                "LudwigXV7.001r.tif",
                "LudwigXV7.001v.tif",
                "LudwigXV7.endmatter.flyleaf.01r.tif",
                "LudwigXV7.endmatter.flyleaf.02v.tif",
                "LudwigXV7.endmatter.pastedown.tif",
                "LudwigXV7.binding.backcover.tif"
        };

        BookImage im1 = new BookImage();
        BookImage im2 = new BookImage();

        for (int i = 0; i < names.length; i++) {
            im1.setId(names[i]);
            for (int j = 0; j < names.length; j++) {
                if (j == i) {
                    continue;
                }
                im2.setId(names[j]);

                if (i < j) {
                    assertTrue(comparator.compare(im1, im2) < 0);
                } else if (i > j) {
                    assertTrue(comparator.compare(im1, im2) > 0);
                } else {
                    assertEquals(0, comparator.compare(im1, im2));
                }
            }
        }
    }

}
