package rosa.archive.model.aor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnnotatedPageTest {

    @Test
    void isForImageComparesNamesWithoutExtension() {
        AnnotatedPage page = new AnnotatedPage();
        page.setPage("Book.frontmatter.pastedown.tif");

        assertTrue(page.isForImage("Book.frontmatter.pastedown.tif"));
        assertTrue(page.isForImage("Book.frontmatter.pastedown.TIF"));
        assertTrue(page.isForImage("Book.frontmatter.pastedown"));
        // A page for a differently named image never matches, even when one name contains the other
        assertFalse(page.isForImage("Book.pastedown.tif"));
        assertFalse(page.isForImage("Book.frontmatter.pastedown.001r.tif"));
        assertFalse(page.isForImage(null));
        assertFalse(new AnnotatedPage().isForImage("Book.001r.tif"));
    }
}
