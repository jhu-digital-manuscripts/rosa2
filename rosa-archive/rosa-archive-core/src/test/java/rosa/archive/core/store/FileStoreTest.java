package rosa.archive.core.store;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.core.check.BookChecker;
import rosa.archive.core.check.BookCollectionChecker;
import rosa.archive.core.check.DataChecker;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 *
 */
public class FileStoreTest {

    private FileStore store;

    @Before
    public void setup() {
        URL u = getClass().getClassLoader().getResource("data/character_names.csv");
        assertNotNull(u);
        String url = u.getPath();

        Path path = Paths.get(url.startsWith("/") ? url.substring(1) : url).getParent().getParent();
        this.store = new FileStore(path.toString(), new DataChecker());
    }

    @Test
    public void listCollectionsTest() {
        String[] collections = store.listBookCollections();
        assertNotNull(collections);

        List<String> list = Arrays.asList(collections);
        assertNotNull(list);
        assertTrue(list.size() > 0);
        assertTrue(list.contains("data"));
    }

    @Test
    public void listBooksTest() {
        String[] books = store.listBooks("data");
        assertNotNull(books);

        List<String> list = Arrays.asList(books);
        assertNotNull(list);
        assertEquals(3, list.size());
        assertTrue(list.contains("Ferrell"));
        assertTrue(list.contains("LudwigXV7"));
        assertTrue(list.contains("Walters143"));
    }

}
