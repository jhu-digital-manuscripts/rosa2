package rosa.archive.core.serialize;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import rosa.archive.model.IllustrationTitles;

/**
 * @see rosa.archive.core.serialize.IllustrationTitlesSerializer
 */
public class IllustrationTitlesSerializerTest extends BaseSerializerTest<IllustrationTitles> {
    @Before
    public void setup() {
        serializer = new IllustrationTitlesSerializer();
    }

    @Test
    public void readTest() throws IOException {
        IllustrationTitles titles = loadResource(COLLECTION_NAME, null, "illustration_titles.csv");

        assertNotNull(titles);
        assertTrue(titles.getAllIds().size() > 0);

        assertEquals(321, titles.getAllIds().size());
        assertEquals("Portrait of Author (Guillaume de Lorris)", titles.getTitleById("1"));
        assertEquals("Dangier Speaks to L’Amans", titles.getTitleById("80"));
    }

    @Test
    public void writeTest() throws IOException {
        // Setup IllustrationTitles object
        IllustrationTitles titles = createObject();

        // Write the object and get all written lines
        List<String> lines = writeObjectAndGetWrittenLines(titles);

        assertNotNull(lines);
        assertEquals(11, lines.size());

        assertEquals("Id,Title", lines.get(0));
        assertTrue(lines.contains("ID3,Title 2"));
        assertTrue(lines.contains("ID8,\"Title 1, Title 2, Title 3\""));
        assertTrue(lines.contains("ID1,Illustration Title [1]"));
    }

    @Override
    protected IllustrationTitles createObject() {
        IllustrationTitles titles = new IllustrationTitles();
//        titles.setId("IllustrationTitles");

        Map<String, String> titlesMap = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            titlesMap.put("ID" + i, "Illustration Title [" + i + "]");
        }
        titlesMap.put("ID3", "Title 2");
        titlesMap.put("ID8", "Title 1, Title 2, Title 3");
        titles.setData(titlesMap);

        return titles;
    }

}
