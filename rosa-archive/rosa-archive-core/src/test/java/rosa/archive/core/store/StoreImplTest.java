package rosa.archive.core.store;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import rosa.archive.core.AbstractFileSystemTest;
import rosa.archive.core.ByteStreamGroup;
import rosa.archive.core.FSByteStreamGroup;
import rosa.archive.core.check.BookChecker;
import rosa.archive.core.check.BookCollectionChecker;
import rosa.archive.core.config.AppConfig;
import rosa.archive.core.serialize.Serializer;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.BookStructure;
import rosa.archive.model.CharacterNames;
import rosa.archive.model.SHA1Checksum;
import rosa.archive.model.CropInfo;
import rosa.archive.model.IllustrationTagging;
import rosa.archive.model.IllustrationTitles;
import rosa.archive.model.ImageList;
import rosa.archive.model.NarrativeSections;
import rosa.archive.model.NarrativeTagging;
import rosa.archive.model.Permission;
import rosa.archive.model.Transcription;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
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
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 */
public class StoreImplTest extends AbstractFileSystemTest {

    private StoreImpl store;
    @Mock
    private AppConfig context;
    @Mock
    private BookCollectionChecker collectionChecker;
    @Mock
    private BookChecker bookChecker;
    private Map<Class, Serializer> serializerMap;

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Before
    public void setup() throws URISyntaxException, IOException {
        super.setup();
        MockitoAnnotations.initMocks(this);
        serializerMap = new HashMap<>();

        this.store = new StoreImpl(serializerMap, bookChecker, collectionChecker, context, base);

        when(
                bookChecker.checkContent(
                        any(BookCollection.class),
                        any(Book.class),
                        any(ByteStreamGroup.class),
                        anyBoolean(),
                        anyList()
                )
        ).thenReturn(false);
        when(
                collectionChecker.checkContent(
                        any(BookCollection.class),
                        any(ByteStreamGroup.class),
                        anyBoolean(),
                        anyList()
                )
        ).thenReturn(false);

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
        when(context.getSHA1SUM()).thenReturn(".SHA1SUM");
    }

    @Test
    public void listCollectionsTest() throws IOException {
        String[] collections = store.listBookCollections();
        assertNotNull(collections);

        List<String> list = Arrays.asList(collections);
        assertNotNull(list);
        assertTrue(list.size() > 0);
        assertTrue(list.contains("data"));
        assertTrue(list.contains("rosedata"));
    }

    @Test
    public void listBooksInRosedata() throws IOException {
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
    public void listBooksTest() throws IOException {
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

        Book book = new Book();
        BookCollection collection = new BookCollection();

        book.setId("LudwigXV7");
        collection.setId("rosedata");

        assertFalse(store.check(collection, book, true, new ArrayList<String>()));
        assertFalse(store.check(collection, book, false, new ArrayList<String>()));
        assertFalse(store.check(collection, true, new ArrayList<String>()));
        assertFalse(store.check(collection, false, new ArrayList<String>()));

        verify(bookChecker).checkContent(eq(collection), eq(book), any(ByteStreamGroup.class), eq(false), anyList());
        verify(bookChecker).checkContent(eq(collection), eq(book), any(ByteStreamGroup.class), eq(true), anyList());

        verify(collectionChecker).checkContent(eq(collection), any(ByteStreamGroup.class), eq(false), anyList());
        verify(collectionChecker).checkContent(eq(collection), any(ByteStreamGroup.class), eq(true), anyList());

    }

    @Test
    public void loadBookTest() throws Exception {
        Set<Class> classes = new HashSet<>();

        classes.add(BookMetadata.class);
        classes.add(BookStructure.class);
        classes.add(SHA1Checksum.class);
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

        List<String> errors = new ArrayList<>();
        for (String bookName : books[0]) {
            mockSerializers(classes);
            Book book = store.loadBook("data", bookName, errors);
            checkBook(book, classes);
        }
        for (String bookName : books[1]) {
            mockSerializers(classes);
            Book book = store.loadBook("rosedata", bookName, errors);
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
        classes.add(NarrativeSections.class);

        String[] cols = {"data", "rosedata"};

        List<String> errors = new ArrayList<>();
        for (String collectionName : cols) {
            mockSerializers(classes);
            BookCollection collection = store.loadBookCollection(collectionName, errors);

            assertNotNull(collection);
            assertNotNull(collection.getId());
            assertNotNull(collection.getCharacterNames());
            assertNotNull(collection.getIllustrationTitles());
            assertNotNull(collection.getNarrativeSections());
            assertNotNull(collection.getAllSupportedLanguages());
        }
    }



    @Test
    @SuppressWarnings("unchecked")
    public void updateChecksumCreatesNewChecksumFile() throws Exception {
        Set<Class> classes = new HashSet<>();
        classes.add(SHA1Checksum.class);
        classes.add(CharacterNames.class);
        classes.add(IllustrationTitles.class);
        classes.add(NarrativeSections.class);
        mockSerializers(classes);

        File folder = tmpFolder.newFolder();
        folder = Files.createDirectory(folder.toPath().resolve("r")).toFile();

        try (
                InputStream in1 = getClass().getClassLoader().getResourceAsStream("data/character_names.csv");
                InputStream in2 = getClass().getClassLoader().getResourceAsStream("data/illustration_titles.csv");
                InputStream in3 = getClass().getClassLoader().getResourceAsStream("data/narrative_sections.csv")
            ) {
            Files.copy(in1, folder.toPath().resolve("character_names.csv"));
            Files.copy(in2, folder.toPath().resolve("illustration_titles.csv"));
            Files.copy(in3, folder.toPath().resolve("narrative_sections.csv"));
        }

        ByteStreamGroup temp = new FSByteStreamGroup(folder.getParent());
        Store tmpStore = new StoreImpl(serializerMap, bookChecker, collectionChecker, context, temp);

        List<String> errors = new ArrayList<>();
        BookCollection collection = tmpStore.loadBookCollection("r", errors);

        assertNotNull(collection);
        assertEquals(0, errors.size());

        tmpStore.updateChecksum(collection, false, errors);
        assertEquals(0, errors.size());

        List<String> inCollection = Arrays.asList(folder.list());
        assertTrue(inCollection.contains("character_names.csv"));
        assertTrue(inCollection.contains("illustration_titles.csv"));
        assertTrue(inCollection.contains("narrative_sections.csv"));

        verify(serializerMap.get(SHA1Checksum.class)).write(anyObject(), any(OutputStream.class));
    }

    @SuppressWarnings("unchecked")
    private void mockSerializers(Set<Class> classes) throws Exception {
        serializerMap.clear();

        for (Class c : classes) {
            Serializer s = mock(Serializer.class);
            when(s.read(any(InputStream.class), any(List.class)))
                    .thenReturn(c.newInstance());
            serializerMap.put(c, s);
        }
    }
}
