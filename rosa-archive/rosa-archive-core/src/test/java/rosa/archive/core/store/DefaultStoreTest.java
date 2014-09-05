package rosa.archive.core.store;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rosa.archive.core.AbstractFileSystemTest;
import rosa.archive.core.ByteStreamGroup;
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

import java.io.IOException;
import java.io.InputStream;
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

        // Setting config to a single constant to ensure that all input streams will open
        // in order to read from the mock serializers.
        final String GOOD_FILE = ".crop.txt";
        when(context.languages()).thenReturn(new String[] {"en", "fr"});
        when(context.getPERMISSION()).thenReturn(".permission_");
        when(context.getDESCRIPTION()).thenReturn(".description_");
        when(context.getIMAGES()).thenReturn(GOOD_FILE);
        when(context.getIMAGES_CROP()).thenReturn(GOOD_FILE);
        when(context.getCHARACTER_NAMES()).thenReturn("character_names.csv");
        when(context.getCROP()).thenReturn(GOOD_FILE);
        when(context.getILLUSTRATION_TITLES()).thenReturn("illustration_titles.csv");
        when(context.getIMAGE_TAGGING()).thenReturn(GOOD_FILE);
        when(context.getMISSING_PAGES()).thenReturn("missing.txt");
        when(context.getNARRATIVE_SECTIONS()).thenReturn("narrative_sections.csv");
        when(context.getSHA1SUM()).thenReturn(GOOD_FILE);
        when(context.getNARRATIVE_TAGGING()).thenReturn(GOOD_FILE);
        when(context.getNARRATIVE_TAGGING_MAN()).thenReturn(GOOD_FILE);
        when(context.getREDUCED_TAGGING()).thenReturn(GOOD_FILE);
        when(context.getTRANSCRIPTION()).thenReturn(".transcription");
        when(context.getXML()).thenReturn(".xml");
    }

    @Test
    public void listCollectionsTest() {
        String[] collections = store.listBookCollections();
        assertNotNull(collections);

        List<String> list = Arrays.asList(collections);
        assertNotNull(list);
        assertTrue(list.size() > 0);
        assertTrue(list.contains("data"));
        assertTrue(list.contains("rosedata"));
    }

    @Test
    public void listBooksInRosedata() {
        String[] books = store.listBooks("rosedata");
        assertNotNull(books);

        List<String> list = Arrays.asList(books);
        assertNotNull(list);
        assertEquals(4, list.size());
        assertTrue(list.contains("Walters143"));
        assertTrue(list.contains("Senshu2"));
        assertTrue(list.contains("Morgan948"));
        assertTrue(list.contains("LudwigXV7"));
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
    public void loadBookTest() throws Exception {
        Set<Class> classes = new HashSet<>();

        classes.add(BookMetadata.class);
        classes.add(BookStructure.class);
        classes.add(ChecksumInfo.class);
        classes.add(CropInfo.class);
        classes.add(IllustrationTagging.class);
        classes.add(ImageList.class);
        classes.add(NarrativeTagging.class);
        classes.add(Permission.class);
        classes.add(Transcription.class);

        String[][] books = {
                {"Ferrell", "LudwigXV7", "Walters143"},
                {"LudwigXV7", "Morgan948", "Senshu2", "Walters143"}
        };

        for (String bookName : books[0]) {
            mockSerializers(classes);
            Book book = store.loadBook("data", bookName);
            checkBook(book, classes);
        }
        for (String bookName : books[1]) {
            mockSerializers(classes);
            Book book = store.loadBook("rosedata", bookName);
            checkBook(book, classes);
        }
    }

    @SuppressWarnings("unchecked")
    private void checkBook(Book book, Set<Class> classes) throws IOException {
        for (Class c : classes) {
            // In the case the a file does not exist, the read() method will not be called...
            verify(serializerMap.get(c), atMost(2)).read(any(InputStream.class), any(List.class));
        }

        assertNotNull(book.getContent());
        assertTrue(book.getContent().length > 0);
        assertNotNull(book.getId());
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
    public void loadCollectionTest() throws Exception {
        Set<Class> classes = new HashSet<>();

        classes.add(CharacterNames.class);
        classes.add(IllustrationTitles.class);
        classes.add(MissingList.class);
        classes.add(NarrativeSections.class);

        String[] cols = {"data", "rosedata"};

        for (String collectionName : cols) {
            mockSerializers(classes);
            BookCollection collection = store.loadBookCollection(collectionName);

            assertNotNull(collection);
            assertNotNull(collection.getId());
            assertNotNull(collection.getCharacterNames());
            assertNotNull(collection.getIllustrationTitles());
            assertNotNull(collection.getNarrativeSections());
            assertNotNull(collection.getMissing());
            assertNotNull(collection.getAllSupportedLanguages());
        }
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
