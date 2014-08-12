package rosa.archive.core.serialize;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.IllustrationTitles;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @see rosa.archive.core.serialize.IllustrationTitlesSerializer
 */
public class IllustrationTitlesSerializerTest {
    private static final String testFile = "illustration_titles.csv";

    private IllustrationTitlesSerializer serializer;

    @Before
    public void setup() {
        this.serializer = new IllustrationTitlesSerializer();
    }

    @Test
    public void readsValidInput() throws Exception {

        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(testFile)) {
            IllustrationTitles titles = serializer.read(stream);

            assertNotNull(titles);
            assertTrue(titles.getAllIds().size() > 0);

            assertEquals(80, titles.getAllIds().size());
            assertEquals("Portrait of Author (Guillaume de Lorris)", titles.getTitleById("1"));
            assertEquals("Dangier Speaks to L’Amans", titles.getTitleById("80"));
        }

    }

    @Test (expected = UnsupportedOperationException.class)
    public void writesDataToFile() throws IOException {
        OutputStream out = mock(OutputStream.class);
        IllustrationTitles titles = mock(IllustrationTitles.class);

        serializer.write(titles, out);
    }

}
