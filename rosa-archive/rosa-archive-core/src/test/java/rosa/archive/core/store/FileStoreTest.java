package rosa.archive.core.store;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rosa.archive.core.check.Checker;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.CharacterNames;
import rosa.archive.model.CropInfo;
import rosa.archive.model.IllustrationTitles;

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
    private Path top;

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
        this.top = path;
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

    @Test
    public void crappyTest() throws Exception {
        Book book = new Book();
        store.setFieldFromFile(
                book,
                "cropInfo",
                top.resolve("data/Walters143/Walters143.crop.txt"),
                new CropInfo()
        );
        store.setFieldFromFile(
                book,
                "bookMetadata",
                top.resolve("data/Walters143/Walters143.description_en.xml"),
                new BookMetadata()
        );
        System.out.println(book.toString());
        assertNotNull(book.getCropInfo());
        assertNotNull(book.getBookMetadata());
    }

    @Test
    public void loadBookTest() {
        Book book = store.loadBook("data", "Walters143");
        System.out.println(book.toString());
    }

    @Test
    public void loadCollectionTest() {
        BookCollection collection = new BookCollection();
        store.setFieldFromFile(
                collection,
                "characterNames",
                top.resolve("data/character_names.csv"),
                new CharacterNames()
        );
        store.setFieldFromFile(
                collection,
                "illustrationTitles",
                top.resolve("data/illustration_titles.csv"),
                new IllustrationTitles()
        );
        System.out.println(collection.toString());
    }

}
