package rosa.archive.model;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 *@see rosa.archive.model.IllustrationTitles
 */
public class IllustrationTitlesTest {
    private static final int MAX_TITLES = 10;

    private IllustrationTitles titles;

    @Before
    public void setup() {
        this.titles = new IllustrationTitles();

        Map<String, String> data = new HashMap<>();
        for (int i = 0; i < MAX_TITLES; i++) {
            String title = "" + i + i;
            data.put(String.valueOf(i), title);
        }
        titles.setData(data);
    }

    @Test
    public void findIdReturnsCorrectId() {
        for (int i = 0; i < MAX_TITLES; i++) {
            String testTitle = "" + i + i;
            String id = titles.findIdOfTitle(testTitle);

            assertNotNull(id);
            assertEquals(String.valueOf(i), id);
        }

        String[] badIds = { "-2", "15", "100" };
        for (String id : badIds) {
            String testTitle = "" + id + id;
            String foundId = titles.findIdOfTitle(testTitle);

            assertNull(foundId);
        }
    }
}
