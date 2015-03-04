package rosa.website.core.client.server;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.core.BaseArchiveTest;
import rosa.website.core.client.ArchiveDataService;
import rosa.website.core.server.ArchiveDataServiceImpl;
import rosa.website.model.csv.CSVEntry;
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
        assertTrue("Illustrations should appear no more than once in the test data.",
                illustrationsAppearNoMoreThanTwice(ills));

//        boolean isFirst = true;
//        for (IllustrationTitleCSV.Column col : IllustrationTitleCSV.Column.values()) {
//            if (!isFirst) {
//                System.out.print(",");
//            }
//            System.out.print(col.key);
//            isFirst = false;
//        }
//        System.out.println();
//        for (CSVEntry entry : ills) {
//            System.out.print(entry.getValue(IllustrationTitleCSV.Column.LOCATION));
//            System.out.print(",");
//            String val = entry.getValue(IllustrationTitleCSV.Column.TITLE);
//            System.out.print(val.contains(",") ? "\"" + val + "\"" : val);
//            System.out.print(",");
//            System.out.print(entry.getValue(IllustrationTitleCSV.Column.FREQUENCY));
//            System.out.println();
//        }
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
