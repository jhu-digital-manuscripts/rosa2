package rosa.archive.core.serialize;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.core.config.AppConfig;
import rosa.archive.model.BookDescription;

import java.io.InputStream;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 *
 */
public class BookDescriptionSerializerTest extends BaseSerializerTest {

    private BookDescriptionSerializer serializer;

    @Before
    public void setup() {
        AppConfig config = mock(AppConfig.class);

        serializer = new BookDescriptionSerializer(config);
    }

    @Test
    public void readTest() throws Exception {
        final String testFile = "data/Walters143/Walters143.description_en.xml";

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(testFile)) {
            BookDescription description = serializer.read(in, errors);

            assertNotNull(description);
            System.out.println(description.toString());
        }

    }

}
