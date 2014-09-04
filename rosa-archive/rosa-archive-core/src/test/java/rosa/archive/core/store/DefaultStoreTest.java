package rosa.archive.core.store;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rosa.archive.core.AbstractFileSystemTest;
import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.ByteStreamGroupImpl;
import rosa.archive.core.check.Checker;
import rosa.archive.core.config.AppConfig;
import rosa.archive.core.serialize.Serializer;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.BookStructure;
import rosa.archive.model.CharacterNames;
import rosa.archive.model.ChecksumInfo;
import rosa.archive.model.CropInfo;
import rosa.archive.model.IllustrationTagging;
import rosa.archive.model.IllustrationTitles;
import rosa.archive.model.ImageList;
import rosa.archive.model.MissingList;
import rosa.archive.model.NarrativeSections;
import rosa.archive.model.NarrativeTagging;
import rosa.archive.model.Permission;
import rosa.archive.model.Transcription;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 */
public class DefaultStoreTest extends AbstractFileSystemTest {

    private DefaultStore store;
    @Mock
    private AppConfig context;
    private Map<Class, Serializer> serializerMap;
    private Map<Class, Checker> checkerMap;

    @Before
    public void setup() {
        super.setup();
        MockitoAnnotations.initMocks(this);
        serializerMap = new HashMap<>();
        checkerMap = new HashMap<>();

        this.store = new DefaultStore(serializerMap, checkerMap, context, base);

        when(context.languages()).thenReturn(new String[] {"en", "fr"});
        when(context.getPERMISSION()).thenReturn(".permission_");
        when(context.getDESCRIPTION()).thenReturn(".description_");
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
    @SuppressWarnings("unchecked")
    public void checkerTests() throws Exception {
        Set<Class> classes = new HashSet<>();
        classes.add(Book.class);
        classes.add(BookCollection.class);

        mockCheckers(classes);

        Book book = new Book();
        BookCollection collection = new BookCollection();

        assertFalse(store.check(book, true));
        assertFalse(store.check(book, false));
        assertFalse(store.check(collection, true));
        assertFalse(store.check(collection, false));

        Checker bChecker = checkerMap.get(Book.class);
        verify(bChecker).checkContent(eq(book), any(ByteStreamGroup.class), eq(true));
        verify(bChecker).checkContent(eq(book), any(ByteStreamGroup.class), eq(true));

        Checker cChecker = checkerMap.get(BookCollection.class);
        verify(cChecker).checkContent(eq(collection), any(ByteStreamGroup.class), eq(true));
        verify(cChecker).checkContent(eq(collection), any(ByteStreamGroup.class), eq(true));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void loadBookTest() throws Exception {
        Set<Class> classes = new HashSet<>();

        classes.add(BookMetadata.class);
        classes.add(BookStructure.class);
        classes.add(CharacterNames.class);
        classes.add(ChecksumInfo.class);
        classes.add(CropInfo.class);
        classes.add(IllustrationTagging.class);
        classes.add(IllustrationTitles.class);
        classes.add(ImageList.class);
        classes.add(MissingList.class);
        classes.add(NarrativeSections.class);
        classes.add(NarrativeTagging.class);
        classes.add(Permission.class);
        classes.add(Transcription.class);

        mockSerializers(classes);

        Book book = store.loadBook("data", "Walters143");
        assertNotNull(book);

        for (Class c : classes) {
            // In the case the a file does not exist, the read() method will not be called...
//            verify(serializerMap.get(c), atLeastOnce()).read(any(InputStream.class));
            verify(serializerMap.get(c), atMost(2)).read(any(InputStream.class), any(List.class));
        }

        assertNotNull(book.getContent());
        assertTrue(book.getContent().length > 0);
        assertNotNull(book.getPermissionsInAllLanguages());
        assertTrue(book.getPermissionsInAllLanguages().length > 0);

    }

    @Test
    public void findsLanguageCode() {
        assertEquals("en", store.findLanguageCodeInName("Walters143.permission_en.xml"));
        assertEquals("fr", store.findLanguageCodeInName("Walters143.permission_fr.xml"));
        assertEquals("eng", store.findLanguageCodeInName("Walters143.permission_eng.xml"));
    }

    @Test
    public void returnEmptyStringForBadLanguage() {
        assertEquals("", store.findLanguageCodeInName("Walters143.permissions.xml"));
        assertEquals("", store.findLanguageCodeInName("Walters143.permissions_en-US.xml"));
        assertEquals("", store.findLanguageCodeInName("Walters143.transcription.001r.txt"));
        assertEquals("", store.findLanguageCodeInName("Walters143.transcription.xml"));
    }

    @Test
    public void loadCollectionTest() {

    }

    @SuppressWarnings("unchecked")
    private void mockSerializers(Set<Class> classes) throws Exception {

        for (Class c : classes) {
            Serializer s = mock(Serializer.class);
            when(s.read(any(InputStream.class), any(List.class)))
                    .thenReturn(c.newInstance());
            serializerMap.put(c, s);
        }
    }

    @SuppressWarnings("unchecked")
    private void mockCheckers(Set<Class> classes) throws Exception {
        for (Class c : classes) {
            Checker check = mock(Checker.class);
            when(check.checkContent(anyObject(), any(ByteStreamGroup.class), anyBoolean()))
                    .thenReturn(false);
            checkerMap.put(c, check);
        }
    }

}
