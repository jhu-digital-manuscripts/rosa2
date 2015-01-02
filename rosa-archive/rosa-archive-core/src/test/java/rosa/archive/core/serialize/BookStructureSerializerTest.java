package rosa.archive.core.serialize;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.BookStructure;
import rosa.archive.model.redtag.StructureColumn;
import rosa.archive.model.redtag.StructurePage;
import rosa.archive.model.redtag.StructurePageSide;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @see rosa.archive.core.serialize.BookStructureSerializer
 * @see rosa.archive.model.BookStructure
 */
public class BookStructureSerializerTest extends BaseSerializerTest<BookStructure> {

    @Before
    public void setup() {
        serializer = new BookStructureSerializer();
    }

    @Test
    public void readTest() throws IOException {
        BookStructure structure = loadResource("data/LudwigXV7/LudwigXV7.redtag.txt");
        assertNotNull(structure);

        List<StructurePage> pages = structure.pages();
        assertNotNull(pages);
        assertEquals(10, pages.size());

        StructurePage page = pages.get(9);
        assertNotNull(page);
        assertEquals("10", page.getId());
        assertEquals("10", page.getName());
        assertNotNull(page.getRecto());
        assertNotNull(page.getVerso());

        StructurePageSide side = page.getRecto();
        assertNotNull(side);
        assertEquals("10r", side.getParentPage());
        assertNotNull(side.columns());
        assertNotNull(side.spanning());
        assertEquals(2, side.columns().size());

        StructureColumn col = side.columns().get(0);
        assertNotNull(col);
        assertNotNull("10r", col.getParentSide());
        assertEquals('a', col.getColumnLetter());
        assertNotNull(col.getItems());
        assertEquals(9, col.getItems().size());

        List<StructureColumn> columns = structure.columns();
        assertNotNull(columns);
        assertEquals(40, columns.size());
    }

    @Test (expected = UnsupportedOperationException.class)
    public void writeTest() throws IOException {
        writeObjectAndGetContent(new BookStructure());
    }

}
