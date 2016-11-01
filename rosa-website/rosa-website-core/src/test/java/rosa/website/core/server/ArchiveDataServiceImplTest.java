package rosa.website.core.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import rosa.archive.core.BaseArchiveTest;
import rosa.website.core.client.ArchiveDataService;
import rosa.website.model.table.BookDataColumn;
import rosa.website.model.table.Table;
import rosa.website.model.table.Row;
import rosa.website.model.table.Tables;
import rosa.website.model.table.IllustrationTitleColumn;

public class ArchiveDataServiceImplTest extends BaseArchiveTest {

    private ArchiveDataService service;

    @Before
    public void setup() {
        StoreAccessLayer accessLayer = new StoreAccessLayerImpl(
                new StoreProvider(serializers, bookChecker, collectionChecker), base.id()
        );
        service = new ArchiveDataServiceImpl(accessLayer, "valid");
    }

    /**
     * Test method for loading and creating the illustration titles CSV data.
     */
    @Test
    public void loadIllustrationTitlesTest() throws Exception {
        Table ills = service.loadCSVData(VALID_COLLECTION, null, Tables.ILLUSTRATIONS);
        assertNotNull("No illustration titles CSV found.", ills);

        // Only those illustrations that appear in Ludwig will show up in this list
        assertEquals("Unexpected number of illustrations found.", 111, ills.rows().size());

        // Illustrations not included simply do not appear.
        assertTrue("Illustrations should appear no more than twice in the test data.",
                illustrationsAppearNoMoreThanTwice(ills));
    }

    /**
     * Test method for collection book data CSV.
     */
    @Test
    public void loadBookDataTest() throws Exception {
        Table data = service.loadCSVData(VALID_COLLECTION, "en", Tables.BOOK_DATA);
        assertNotNull("Collection data CSV missing.", data);
        assertEquals(2, data.rows().size());

        Row row = data.getRow(0);
        
        assertEquals("Unexpected book ID found.", "FolgersHa2", row.getValue(BookDataColumn.ID));
        assertEquals("Unexpected book repo found.", "Folger Shakespeare Library", row.getValue(BookDataColumn.REPO));
        assertEquals("Unexpected book shelfmark found.", "H.a.2 (ms. content)", row.getValue(BookDataColumn.SHELFMARK));
        assertEquals("Unexpected book common name found", "Princeton's Facetie", row.getValue(BookDataColumn.COMMON_NAME));
        assertEquals("unexpected book current location found.", "Folger Shakespeare Library", row.getValue(BookDataColumn.CURRENT_LOCATION));
        assertEquals("Unexpected date found.", "1571", row.getValue(BookDataColumn.DATE));
        assertEquals("Unexpected origin found.", "", row.getValue(BookDataColumn.ORIGIN));
        assertEquals("Unexpected type found.", "", row.getValue(BookDataColumn.TYPE));
        assertEquals("Unexpected number of illustrations found.", "0", row.getValue(BookDataColumn.NUM_ILLUS));
        assertEquals("Unexpected number of pages found.", "211", row.getValue(BookDataColumn.NUM_FOLIOS));
        
        row = data.getRow(1);
        assertEquals("Unexpected book ID found.", "LudwigXV7", row.getValue(BookDataColumn.ID));
        assertEquals("Unexpected book repo found.", "J. Paul Getty Museum", row.getValue(BookDataColumn.REPO));
        assertEquals("Unexpected book shelfmark found.", "Ludwig XV 7", row.getValue(BookDataColumn.SHELFMARK));
        assertEquals("Unexpected book common name found", "Ludwig XV7", row.getValue(BookDataColumn.COMMON_NAME));
        assertEquals("unexpected book current location found.", "Los Angeles", row.getValue(BookDataColumn.CURRENT_LOCATION));
        assertEquals("Unexpected date found.", "15th century", row.getValue(BookDataColumn.DATE));
        assertEquals("Unexpected origin found.", "Paris, France", row.getValue(BookDataColumn.ORIGIN));
        assertEquals("Unexpected type found.", "manuscript", row.getValue(BookDataColumn.TYPE));
        assertEquals("Unexpected number of illustrations found.", "101", row.getValue(BookDataColumn.NUM_ILLUS));
        assertEquals("Unexpected number of pages found.", "135", row.getValue(BookDataColumn.NUM_FOLIOS));
        
    }

    /**
     * Illustrations can appear more than once if they have multiple entries in the
     * book collection's 'illustration_titles.csv'
     */
    private boolean illustrationsAppearNoMoreThanTwice(Table ills) {
        for (Row entry : ills.rows()) {
            int freq = Integer.parseInt(entry.getValue(IllustrationTitleColumn.FREQUENCY));

            if (freq > 2) {
                return false;
            }
        }
        return true;
    }

}
