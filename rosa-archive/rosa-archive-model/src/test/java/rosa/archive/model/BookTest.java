package rosa.archive.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

/**
 * Nothing but getters/setters currently. Not much to test...
 *
 * @see rosa.archive.model.Book
 */
public class BookTest {

    private Book book;

    @Before
    public void setup() {
        this.book = new Book();
    }

    @Test
    public void getAllPermissionsTest() {
        Permission p1 = new Permission();
        p1.setPermission("English permission statement!");
        Permission p2 = new Permission();
        p2.setPermission("French permission statement");
        Permission p3 = new Permission();
        p3.setPermission("German permission statement.");

        book.addPermission(p1, "en");
        book.addPermission(p2, "fr");
        book.addPermission(p3, "de");

        Permission[] perms = book.getPermissionsInAllLanguages();
        assertNotNull(perms);
        assertEquals(3, perms.length);
    }
}
