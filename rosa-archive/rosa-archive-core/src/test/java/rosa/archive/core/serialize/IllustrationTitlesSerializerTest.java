package rosa.archive.core.serialize;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.IllustrationTitles;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @see rosa.archive.core.serialize.IllustrationTitlesSerializer
 */
public class IllustrationTitlesSerializerTest extends BaseSerializerTest {
    private static final String testFile = "data/illustration_titles.csv";

    private Serializer<IllustrationTitles> serializer;

    @Before
    public void setup() {
        super.setup();
        serializer = new IllustrationTitlesSerializer(config);
    }

    @Test
    public void readsValidInput() throws Exception {

        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(testFile)) {
            IllustrationTitles titles = serializer.read(stream, errors);

            assertNotNull(titles);
            assertTrue(titles.getAllIds().size() > 0);

            assertEquals(321, titles.getAllIds().size());
            assertEquals("Portrait of Author (Guillaume de Lorris)", titles.getTitleById("1"));
            assertEquals("Dangier Speaks to L’Amans", titles.getTitleById("80"));
        }

    }

    @Test
    public void writesDataToFile() throws IOException {
        IllustrationTitles titles = new IllustrationTitles();
        titles.setId("IllustrationTitles");

        Map<String, String> titlesMap = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            titlesMap.put("ID" + i, "Illustration Title [" + i + "]");
        }
        titles.setData(titlesMap);

        serializer.write(titles, System.out);
    }

}
