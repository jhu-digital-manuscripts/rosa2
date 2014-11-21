package rosa.archive.model;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.redtag.StructurePage;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @see rosa.archive.model.BookStructure
 */
public class BookStructureTest {
    private static final int MAX_PAGES = 10;

    private BookStructure structure;

    @Before
    public void setup() {
        this.structure = new BookStructure();

        List<StructurePage> pages = structure.pages();
//        for (int i = 0; i < MAX_PAGES; i++) {
//            StructurePage page = mock(StructurePage.class);
//            when(page.getId()).thenReturn(String.valueOf(i));
//            when(page.getName()).thenReturn(String.valueOf(i));
//
//            StructurePageSide recto = mock(StructurePageSide.class);
//            when(page.getRecto()).thenReturn(recto);
//            StructurePageSide verso = mock(StructurePageSide.class);
//            when(page.getVerso()).thenReturn(verso);
//
//            pages.add(page);
//
//            StructureColumn col1 = mock(StructureColumn.class);
//            StructureColumn col2 = mock(StructureColumn.class);
//            StructureColumn col3 = mock(StructureColumn.class);
//            StructureColumn col4 = mock(StructureColumn.class);
//
//            when(recto.columns()).thenReturn(Arrays.asList(col1, col2));
//            when(verso.columns()).thenReturn(Arrays.asList(col3, col4));
//        }

        for (int i = 0; i < MAX_PAGES; i++) {
            StructurePage page = new StructurePage(String.valueOf(i), 10);
            pages.add(page);
        }
    }

    @Test
    public void returnsCorrectIndexForValidPage() {
        String[] goodIds = { "2", "4", "8" };
        for (String id : goodIds) {
            int index = structure.findIndex(id);
            assertTrue(index >= 0);
            assertTrue(index < MAX_PAGES);
            assertEquals(Integer.parseInt(id), index);
        }
    }

    @Test
    public void returnsInvalidIndexForInvalidPage() {
        String[] badIds = { "-2", "15", "100" };
        for (String id : badIds) {
            int index = structure.findIndex(id);
            assertEquals(-1, index);
        }
    }

    @Test
    public void returnsAllColumns() {
        assertEquals(MAX_PAGES * 4, structure.columns().size());
    }

    @Test
    public void loopsThroughAllPages() {
        int count = 0;
        for (StructurePage page : structure) {
            int id = Integer.parseInt(page.getId());

            assertTrue(id >= 0);
            assertTrue(id < MAX_PAGES);

            count++;
        }
        assertTrue(count == MAX_PAGES);
    }
}
