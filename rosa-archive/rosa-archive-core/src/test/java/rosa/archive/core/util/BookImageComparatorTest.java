package rosa.archive.core.util;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.BookImage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test this godawful comparator.
 */
public class BookImageComparatorTest {

    private BookImageComparator comparator;

    @Before
    public void setup() {
        comparator = BookImageComparator.instance();
    }

    /**
     * Manuscript pagination: 001r, 001v, 002r, 002v, etc
     *
     * Page names are already in order. Each page name is compared to every other
     * page name and the results of the comparator are checked.
     */
    @Test
    public void manuscriptPaginationTest() {
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

        testNames(names);
    }

    /**
     * Example printed book pagination: A1r, A1v, A2r, A2v, ..., C3v, C4r, ..., etc
     */
    @Test
    public void printedBookPaginationTest() {
        String[] names = {
                "id.a1r.tif", "id.A1v", "id.C3v", "id.C7r"
        };

        testNames(names);
    }

    private void testNames(String[] names) {
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
                    assertTrue(comparator.compare(im2, im1) > 0);
                } else if (i > j) {
                    assertTrue(comparator.compare(im1, im2) > 0);
                    assertTrue(comparator.compare(im2, im1) < 0);
                } else {
                    assertEquals(0, comparator.compare(im1, im2));
                    assertEquals(0, comparator.compare(im2, im1));
                }
            }
        }
    }

}
