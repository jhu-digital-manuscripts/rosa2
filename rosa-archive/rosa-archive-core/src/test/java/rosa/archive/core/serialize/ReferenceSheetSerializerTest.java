package rosa.archive.core.serialize;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import rosa.archive.model.ReferenceSheet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ReferenceSheetSerializerTest extends BaseSerializerTest<ReferenceSheet> {

    @Before
    public void setup() {
        serializer = new ReferenceSheetSerializer();
    }

    @Override
    public void readTest() throws IOException {
        final String file = "people.csv";
        List<String> errors = new ArrayList<>();

        ReferenceSheet sheet = loadResource(COLLECTION_NAME, null, file, errors);
        assertNotNull(sheet);
        assertTrue(errors.isEmpty());

        for (String key : sheet.getKeys()) {
            List<String> vals = sheet.getAlternates(key);

            if (sheet.hasAlternates(key)) {
                for (String val : vals) {
                    assertNotNull("NULL value for key. [" + key + "]", val);
                    assertFalse("Empty value for key. [" + key + "]", val.isEmpty());
                }
            }
        }
    }

    @Override
    @Test (expected = UnsupportedOperationException.class)
    public void writeTest() throws IOException {
        serializer.write(null, null);
    }

    @Override
    @Ignore
    public void roundTripTest() throws IOException {}

    @Override
    protected ReferenceSheet createObject() {
        return null;
    }
}
