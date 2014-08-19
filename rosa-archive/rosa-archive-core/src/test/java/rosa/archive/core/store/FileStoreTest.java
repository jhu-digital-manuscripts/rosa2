package rosa.archive.core.store;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rosa.archive.core.check.Checker;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 *
 */
public class FileStoreTest {

    private FileStore store;

    @Mock
    private Checker<Object> checker;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(checker.checkBits(anyObject())).thenReturn(false);
        when(checker.checkContent(anyObject())).thenReturn(false);

        URL u = getClass().getClassLoader().getResource("data/character_names.csv");
        assertNotNull(u);
        String url = u.getPath();

        Path path = Paths.get(url.startsWith("/") ? url.substring(1) : url).getParent().getParent();
        this.store = new FileStore(path.toString(), checker);
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

    @Test
    public void checkerTests() {
        Book book = new Book();
        BookCollection collection = new BookCollection();

        assertFalse(store.checkBitIntegrity(book));
        assertFalse(store.checkBitIntegrity(collection));
        assertFalse(store.checkContentConsistency(book));
        assertFalse(store.checkContentConsistency(collection));

        verify(checker).checkBits(book);
        verify(checker).checkBits(collection);
        verify(checker).checkContent(book);
        verify(checker).checkContent(collection);
    }

}
