package rosa.website.core.client.server;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.core.BaseArchiveTest;
import rosa.website.core.client.ArchiveDataService;
import rosa.website.core.server.ArchiveDataServiceImpl;
import rosa.website.model.csv.CSVEntry;
import rosa.website.model.csv.CollectionCSV;
import rosa.website.model.csv.IllustrationTitleCSV;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ArchiveDataServiceImplTest extends BaseArchiveTest {

    private ArchiveDataService service;

    @Before
    public void setup() {
        service = new ArchiveDataServiceImpl(store);
    }

    @Test
    public void loadIllustrationTitlesTest() throws IOException {
        IllustrationTitleCSV ills = service.loadIllustrationTitles(VALID_COLLECTION);
        assertNotNull("No illustration titles CSV found.", ills);

        assertEquals("Unexpected number of illustrations found.", 321, ills.size());
        assertEquals("Exactly '210' illustrations should not be present in test data.", 210,
                countIllustrationsNotInTestData(ills));
        assertTrue("Illustrations should appear no more than twice in the test data.",
                illustrationsAppearNoMoreThanTwice(ills));
    }

    @Test
    public void loadCollectionCSVTest() throws IOException {
        CollectionCSV col = service.loadCollectionData(VALID_COLLECTION, "en");
        assertNotNull("Collection CSV data missing.", col);
        assertEquals("Unexpected number of rows found.", 2, col.size());

        CSVEntry row = col.getRow(0);
        assertNotNull("Row missing for index '0'", row);
        assertEquals("Unexpected book ID found.", "LudwigXV7", row.getValue(CollectionCSV.Column.ID));
        assertEquals("Unexpected book name found.", "Ludwig XV7", row.getValue(CollectionCSV.Column.NAME));
        assertEquals("Unexpected book origin found.", "Paris, France", row.getValue(CollectionCSV.Column.ORIGIN));
        assertEquals("Unexpected book material found.", "Parchment", row.getValue(CollectionCSV.Column.MATERIAL));
        assertEquals("Unexpected number of pages found.", "135", row.getValue(CollectionCSV.Column.NUM_FOLIOS));
        assertEquals("Unexpected book height found.", "370", row.getValue(CollectionCSV.Column.HEIGHT));
        assertEquals("Unexpected book width found.", "260", row.getValue(CollectionCSV.Column.WIDTH));
        assertEquals("Unexpected number of leaves per gathering found.", "8", row.getValue(CollectionCSV.Column.LEAVES_PER_GATHERING));
        assertEquals("Unexpected number of lines per column found.", "44", row.getValue(CollectionCSV.Column.LINES_PER_COLUMN));
        assertEquals("Unexpected number of illustrations found.", "101", row.getValue(CollectionCSV.Column.NUM_ILLUS));
        assertEquals("Unexpected start date found.", "1400", row.getValue(CollectionCSV.Column.DATE_START));
        assertEquals("Unexpected end date found.", "1500", row.getValue(CollectionCSV.Column.DATE_END));
        assertEquals("Unexpected number of columns per page found.", "2", row.getValue(CollectionCSV.Column.COLUMNS_PER_FOLIO));
        assertEquals("Unexpected number of texts found.", "1", row.getValue(CollectionCSV.Column.TEXTS));
        assertEquals("Unexpected number of pages with exactly one illustration found.", "79", row.getValue(CollectionCSV.Column.FOLIOS_ONE_ILLUS));
        assertEquals("Unexpected number of pages with more than one illustration found.", "10", row.getValue(CollectionCSV.Column.FOLIOS_MORE_ILLUS));

        row = col.getRow(1);
        assertNotNull("Row missing for index '0'", row);
        assertEquals("Unexpected book ID found.", "FolgersHa2", row.getValue(CollectionCSV.Column.ID));
        assertEquals("Unexpected book name found.", "Princeton's Facetie", row.getValue(CollectionCSV.Column.NAME));
        assertEquals("Unexpected book origin found.", "", row.getValue(CollectionCSV.Column.ORIGIN));
        assertEquals("Unexpected book material found.", "", row.getValue(CollectionCSV.Column.MATERIAL));
        assertEquals("Unexpected number of pages found.", "211", row.getValue(CollectionCSV.Column.NUM_FOLIOS));
        assertEquals("Unexpected book height found.", "110", row.getValue(CollectionCSV.Column.HEIGHT));
        assertEquals("Unexpected book width found.", "160", row.getValue(CollectionCSV.Column.WIDTH));
        assertEquals("Unexpected number of leaves per gathering found.", "-1", row.getValue(CollectionCSV.Column.LEAVES_PER_GATHERING));
        assertEquals("Unexpected number of lines per column found.", "-1", row.getValue(CollectionCSV.Column.LINES_PER_COLUMN));
        assertEquals("Unexpected number of illustrations found.", "-1", row.getValue(CollectionCSV.Column.NUM_ILLUS));
        assertEquals("Unexpected start date found.", "1571", row.getValue(CollectionCSV.Column.DATE_START));
        assertEquals("Unexpected end date found.", "1571", row.getValue(CollectionCSV.Column.DATE_END));
        assertEquals("Unexpected number of columns per page found.", "-1", row.getValue(CollectionCSV.Column.COLUMNS_PER_FOLIO));
        assertEquals("Unexpected number of texts found.", "2", row.getValue(CollectionCSV.Column.TEXTS));
        assertEquals("Unexpected number of pages with exactly one illustration found.", "-1", row.getValue(CollectionCSV.Column.FOLIOS_ONE_ILLUS));
        assertEquals("Unexpected number of pages with more than one illustration found.", "-1", row.getValue(CollectionCSV.Column.FOLIOS_MORE_ILLUS));
    }

    private int countIllustrationsNotInTestData(IllustrationTitleCSV ills) {
        int count = 0;
        for (CSVEntry entry : ills) {
            if (entry.getValue(IllustrationTitleCSV.Column.LOCATION).equals("-1")) {
                count++;
            }
        }
        return count;
    }

    /**
     * Illustrations can appear more than once if they have multiple entries in the
     * book collection's 'illustration_titles.csv'
     */
    private boolean illustrationsAppearNoMoreThanTwice(IllustrationTitleCSV ills) {
        for (CSVEntry entry : ills) {
            int freq = Integer.parseInt(entry.getValue(IllustrationTitleCSV.Column.FREQUENCY));

            if (freq > 2) {
                return false;
            }
        }
        return true;
    }

}
