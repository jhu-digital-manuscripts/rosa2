package rosa.archive.model;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @see rosa.archive.model.IllustrationTagging
 */
public class IllustrationTaggingTest {
    private static final int MAX_ILLS = 10;

    private IllustrationTagging tagging;

    @Before
    public void setup() {
        this.tagging = new IllustrationTagging();

        for (int i = 0; i < MAX_ILLS; i++) {
            Illustration data = mock(Illustration.class);
            when(data.getId()).thenReturn(String.valueOf(i));

            tagging.addIllustrationData(data);
        }
    }

    @Test
    public void returnsCorrectNumberOfIllustrations() {
        assertEquals(MAX_ILLS, tagging.size());

        IllustrationTagging moreTags = new IllustrationTagging();
        assertEquals(0, moreTags.size());
    }

    @Test
    public void returnsCorrectDataForValidIndex() {
        for (int i = 0; i < MAX_ILLS; i++) {
            Illustration data = tagging.getIllustrationData(i);

            assertNotNull(data);
            assertEquals(String.valueOf(i), data.getId());
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void returnsNullForInvalidIndex() {
        int[] badIds = { -2, 15, 100 };

        for (int id : badIds) {
            // This call should throw an exception!
            tagging.getIllustrationData(id);
        }
    }

    @Test
    public void returnsCorrectIndexForIllustrationID() {
        for (int i = 0; i < MAX_ILLS; i++) {
            assertEquals(i, tagging.getIndexOfIllustration(String.valueOf(i)));
        }

        int[] badIds = { -2, 15, 100 };
        for (int id : badIds) {
            assertEquals(-1, tagging.getIndexOfIllustration(String.valueOf(id)));
        }
    }

    @Test
    public void shouldLoopThroughAllData() {
        int count = 0;

        for (Illustration data : tagging) {
            assertNotNull(data);
            count++;
        }

        assertEquals(MAX_ILLS, count);
        assertEquals(MAX_ILLS, tagging.size());
    }

}
