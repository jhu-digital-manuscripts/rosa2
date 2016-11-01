package rosa.website.core.server;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rosa.archive.core.BaseArchiveTest;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookMetadata;
import rosa.website.core.client.ArchiveDataService;
import rosa.website.model.select.BookSelectList;
import rosa.website.model.select.SelectCategory;
import rosa.website.model.table.Table;
import rosa.website.model.table.Row;
import rosa.website.model.table.Tables;

import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * The underlying data Store is mocked in order to fudge some data
 * to simulate less than ideal situations in the archive.
 */
public class ArchiveDataServiceImplTestWithMocks extends BaseArchiveTest {
    private static final String LANGUAGE = "en";

    private ArchiveDataService service;

    @Mock
    private StoreAccessLayer mockStore;

    /**  */
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        service = new ArchiveDataServiceImpl(mockStore, VALID_COLLECTION);
        when(mockStore.hasCollection(VALID_COLLECTION)).thenReturn(true);
    }

    /**
     * If the Store fails to load a BookCollection and instead returns NULL,
     * the #loadCollectionData should also return NULL without throwing any
     * exceptions. A SEVERE log message will appear.
     *
     * @throws IOException
     */
    @Test
    public void loadCollectionDataWithNullCollectionTest() throws IOException {
        when(mockStore.collection(anyString())).thenReturn(null);

        Table data = service.loadCSVData(VALID_COLLECTION, LANGUAGE, Tables.COLLECTION_DISPLAY);
        assertNull("Null data expected.", data);
    }

    /**
     * The service is used to load a particular collection that includes
     * the name of a book that does not exist in the archive. The resulting collection
     * CSV should exist (not be NULL), but have no rows.
     *
     * @throws IOException
     */
    @Test
    public void loadCollectionDataWithInvalidBook() throws IOException {
        BookCollection col = loadValidCollection();
        when(mockStore.collection(anyString())).thenReturn(col);

        when(mockStore.book(anyString(), anyString())).thenReturn(store.loadBook(col, "INVALID_BOOK", null));

        Table collectionCSV = service.loadCSVData(VALID_COLLECTION, LANGUAGE, Tables.COLLECTION_DISPLAY);
        assertEquals("Unexpected number of rows found.", 0, collectionCSV.rows().size());
    }

    /**
     * Assume the service is used to load the collection data for one particular
     * collection, which contains only a single book. The resulting CSV should
     * contain only a single row representing that book.
     *
     * @throws IOException
     */
    @Test
    public void loadCollectionDataWhenCollectionHasOneBook() throws IOException {
        BookCollection mockCollection = new BookCollection();
        mockCollection.setId("collection");
        mockCollection.setBooks(new String[]{"book1"});

        // Load mockCollection when a collection is requested.
        when(mockStore.collection(anyString())).thenReturn(mockCollection);
        when(mockStore.hasCollection("collection")).thenReturn(true);
        // Load LudwigXV7 from test data when a book is requested.
        when(mockStore.book(anyString(), anyString())).thenReturn(loadValidLudwigXV7());
        when(mockStore.hasBook(anyString(), anyString())).thenReturn(true);

        Table collectionCSV = service.loadCSVData("collection", LANGUAGE, Tables.COLLECTION_DISPLAY);

        assertNotNull("Collection CSV data is missing.", collectionCSV);
        assertEquals("Unexpected number of CSV rows found.", 1, collectionCSV.rows().size());
    }

    /**
     * The service is used to load a particular collection that contains only
     * one book. This book is missing the 'BookTexts' in its metadata. The resulting
     * CSV should have one row.
     *
     * @throws IOException
     */
    @Test
    public void loadCollectionDataWithBookMissingTexts() throws IOException {
        BookCollection mockCollection = new BookCollection();
        mockCollection.setId("collection");
        mockCollection.setBooks(new String[]{"book1"});

        // Load mockCollection when a collection is requested.
        when(mockStore.collection(anyString())).thenReturn(mockCollection);
        when(mockStore.hasCollection(anyString())).thenReturn(true);
        // Load LudwigXV7 from test data when a book is requested.
        Book book = loadValidLudwigXV7();

        // Simulate the book having no 'texts' or <msItem>
        BookMetadata md = book.getBookMetadata(LANGUAGE);
        md.setTexts(null);

        when(mockStore.book(anyString(), anyString())).thenReturn(book);
        when(mockStore.hasBook(anyString(), anyString())).thenReturn(true);

        Table collectionCSV = service.loadCSVData("collection", LANGUAGE, Tables.COLLECTION_DISPLAY);
        assertEquals("Unexpected number of rows found.", 1, collectionCSV.rows().size());
    }

    /**
     * Load a collection that contains a book that has no metadata file associated
     * with it. The resulting CSV should exist with a row representing the book,
     * but the row will contain only the book's name.
     *
     * @throws IOException
     */
    @Test
    public void loadCollectionDataWithMissingMetadata() throws IOException {
        BookCollection mockCollection = new BookCollection();
        mockCollection.setId("collection");
        mockCollection.setBooks(new String[]{"book1"});

        // Load mockCollection when a collection is requested.
        when(mockStore.collection(anyString())).thenReturn(mockCollection);
        when(mockStore.hasCollection(anyString())).thenReturn(true);
        // Load LudwigXV7 from test data when a book is requested.
        Book book = loadValidLudwigXV7();
        book.setBookMetadata(new HashMap<String, BookMetadata>());
        assertNull("Metadata should be NULL", book.getBookMetadata(LANGUAGE));

        when(mockStore.book(anyString(), anyString())).thenReturn(book);
        when(mockStore.hasBook(anyString(), anyString())).thenReturn(true);

        Table collectionCSV = service.loadCSVData("collection", LANGUAGE, Tables.COLLECTION_DISPLAY);
        assertEquals("Unexpected number of rows found.", 1, collectionCSV.rows().size());

        Row row = collectionCSV.getRow(0);
        assertNotNull(row);
        for (int i = 0; i < collectionCSV.columns().length; i++) {
            if (i == 0) {
                assertNotNull("Non-NULL value expected.", row.getValue(i));
                assertFalse("Non-empty value expected.", row.getValue(i).isEmpty());
            } else {
                assertTrue("Empty value expected.", row.getValue(i).isEmpty());
            }
        }
    }


    /**
     * Book selection data is loaded for some criteria for a collection that
     * does not exist in the archive. NULL should be returned with no error.
     *
     * @throws IOException
     */
    @Test
    public void bookSelectionDataWithBadCollection() throws IOException {
        when(mockStore.collection(anyString())).thenReturn(null);
        for (SelectCategory category : SelectCategory.values()) {
            assertNull("NULL value expected.", service.loadBookSelectionData(VALID_COLLECTION, category, LANGUAGE));
        }
    }

    /**
     * Load book selection data for criteria for a collection that contains a
     * book that is not in the archive. The result should skip the bad book.
     * Since this mock collection will contain only the bad book, the resulting
     * list should contain no entries.
     *
     * @throws IOException
     */
    @Test
    public void bookSelectionDataWithBadBook() throws IOException {
        final String COL_NAME = "COLLECTION";
        BookCollection mockCollection = new BookCollection();
        mockCollection.setId(COL_NAME);
        mockCollection.setBooks(new String[]{"BAD_NAME"});

        when(mockStore.collection(anyString())).thenReturn(mockCollection);
        when(mockStore.hasCollection(anyString())).thenReturn(true);
        when(mockStore.book(anyString(), anyString())).thenReturn(
                store.loadBook(mockCollection, "BAD_NAME", null));
        when(mockStore.hasBook(anyString(), anyString())).thenReturn(true);

        for (SelectCategory category : SelectCategory.values()) {
            BookSelectList data = service.loadBookSelectionData(COL_NAME, category, LANGUAGE);
            assertNotNull("Result should not be NULL.", data);
            assertEquals("Unexpected collection name found.", COL_NAME, data.getCollection());
            assertEquals("Unexpected book select category found.", category, data.getCategory());
            assertNotNull("Data list cannot be NULL.", data.asList());
            assertEquals("Unexpected number of rows found.", 0, data.asList().size());
        }
    }

    /**
     * Make sure that the service call goes through with no error when the book
     * has no illustration tagging.
     *
     * {@link ArchiveDataService#loadFSIViewerModel(String, String, String)}
     *
     * @throws IOException
     */
    @Test
    public void getFSIViewerModelWithNoIllustrations() throws IOException {
        Book fakeBook = loadValidLudwigXV7();
        fakeBook.setIllustrationTagging(null);

        when(mockStore.collection(anyString())).thenReturn(loadValidCollection());
        when(mockStore.book(anyString(), anyString())).thenReturn(fakeBook);
        when(mockStore.hasBook(anyString(), anyString())).thenReturn(true);

        service.loadFSIViewerModel(VALID_COLLECTION, "any book", "en");
    }
}
