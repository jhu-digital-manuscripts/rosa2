package rosa.archive.model;

import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @see rosa.archive.model.CropInfo
 */
public class CropInfoTest {
    private static final int MAX_PAGES = 10;

    private CropInfo cropInfo;

    @Before
    public void setup() {
        this.cropInfo = new CropInfo();

        for (int i = 0; i < MAX_PAGES; i++) {
            CropData datum = mock(CropData.class);
            when(datum.getId()).thenReturn(String.valueOf(i));

            cropInfo.addCropData(datum);
        }
    }

    @Test
    public void getCropDataForPageWorksForGoodPage() {
        String[] ids = { "2", "4", "8", "0" };

        for (String testId : ids) {
            CropData data = cropInfo.getCropDataForPage(testId);

            assertNotNull(data);
            assertTrue(Integer.parseInt(data.getId()) >= 0);
            assertTrue(Integer.parseInt(data.getId()) < MAX_PAGES);
        }
    }

    @Test
    public void getCropDataForPageReturnsNullForMissingPage() {
        String[] ids = { "-4", "10", "21" };

        for (String testId : ids) {
            CropData data = cropInfo.getCropDataForPage(testId);

            assertNull(data);
        }
    }

    @Test
    public void forEachLoopsThroughAllData() {
        int count = 0;

        for (CropData data : cropInfo) {
            assertNotNull(data);
            assertTrue(Integer.parseInt(data.getId()) >= 0);
            assertTrue(Integer.parseInt(data.getId()) < MAX_PAGES);

            count++;
        }

        assertTrue(count == MAX_PAGES);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void iteratorRemoveThrowsException() {
        Iterator<CropData> it = cropInfo.iterator();

        while(it.hasNext()) {
            CropData data = it.next();
            assertNotNull(data);
            it.remove();
        }
    }

}
