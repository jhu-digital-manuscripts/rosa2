package rosa.iiif.presentation.core.extras;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.core.BaseArchiveTest;
import rosa.iiif.presentation.core.extres.HtmlDecorator;
import rosa.iiif.presentation.core.extres.ISNIResourceDb;

import java.util.Arrays;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ISNIResourceDbTest extends BaseArchiveTest {
    private ISNIResourceDb isni_db;
    private HtmlDecorator decorator;

    @Before
    public void setup() throws Exception {
        isni_db = new ISNIResourceDb(loadValidCollection());
        decorator = new HtmlDecorator();
    }

    @Test
    public void moo() {
        String start = "Jean Bodin, Nikolaus Vigel, Marcantonio Gandino, Theodor Zwinger";
        Arrays.stream(start.split(", ")).forEach(n -> assertNotNull(isni_db.lookup(n)));

        String result = decorator.decorate(start, isni_db);
        assertTrue(result.contains("isni.org/isni"));
    }

}
