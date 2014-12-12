package rosa.archive.model;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.meta.BiblioData;
import rosa.archive.model.meta.MultilangMetadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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

    @Test
    public void getMetadataWithMultilangMetadata() {
        MultilangMetadata mm = createMM();
        Book book = new Book();
        book.setMultilangMetadata(mm);

        BookMetadata inEnglish = book.getBookMetadata("en");
        assertNotNull(inEnglish);
        assertEquals("MM_ID", inEnglish.getId());
        assertEquals(1900, inEnglish.getYearStart());
        assertEquals(1920, inEnglish.getYearEnd());
        assertEquals("mm", inEnglish.getDimensionUnits());
        assertEquals(100, inEnglish.getWidth());
        assertEquals(150, inEnglish.getHeight());
        assertEquals(1, inEnglish.getTexts().length);
        assertEquals("BibTitle", inEnglish.getTitle());
        assertEquals("Todays Date", inEnglish.getDate());
        assertEquals("MSEL", inEnglish.getRepository());
        assertEquals("Over there", inEnglish.getShelfmark());
    }

    private MultilangMetadata createMM() {
        MultilangMetadata mm = new MultilangMetadata();
        mm.setId("MM_ID");
        mm.setYearStart(1900);
        mm.setYearEnd(1920);
        mm.setDimensionUnits("mm");
        mm.setWidth(100);
        mm.setHeight(150);
        mm.setNumberOfPages(1);
        mm.setNumberOfIllustrations(250);

        BookText t1 = new BookText();
        t1.setLinesPerColumn(1);
        t1.setColumnsPerPage(2);
        t1.setLeavesPerGathering(3);
        t1.setNumberOfIllustrations(249);
        t1.setNumberOfPages(7);
        t1.setId("T1_ID");
        t1.setTextId("TEXT_ID");
        t1.setTitle("title_title");
        t1.setFirstPage("1r");
        t1.setLastPage("1b");

        mm.setBookTexts(Arrays.asList(t1));

        BiblioData b1 = new BiblioData();
        b1.setTitle("BibTitle");
        b1.setDateLabel("Todays Date");
        b1.setCurrentLocation("Right here, right now.");
        b1.setRepository("MSEL");
        b1.setShelfmark("Over there");
        b1.setOrigin("My brain");
        b1.setType("compliment");
        b1.setCommonName("threat");
        b1.setMaterial("matter");
        b1.setLanguage("en");

        BiblioData b2 = new BiblioData();
        b2.setTitle("FrBibTitle");
        b2.setLanguage("fr");
        b2.setCurrentLocation("asdf");

        Map<String, BiblioData> map = new HashMap<>();
        map.put("en", b1);
        map.put("fr", b2);
        mm.setBiblioDataMap(map);

        return mm;
    }

}
