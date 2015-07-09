package rosa.website.core.server;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rosa.archive.core.BaseArchiveTest;
import rosa.archive.core.Store;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookMetadata;
import rosa.website.core.client.ArchiveDataService;
import rosa.website.model.csv.BookDataCSV;
import rosa.website.model.csv.CSVRow;
import rosa.website.model.csv.CollectionCSV;
import rosa.website.model.select.BookSelectList;
import rosa.website.model.select.SelectCategory;

import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
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

        service = new ArchiveDataServiceImpl(mockStore);
    }

    /**
     * If the Store fails to load a BookCollection and instead returns NULL,
     * the #loadCollectionData should also return NULL without throwing any
     * exceptions. A SEVERE log message will appear.
     *
     * @throws IOException .
     */
    @Test
    public void loadCollectionDataWithNullCollectionTest() throws IOException {
        when(mockStore.collection(anyString())).thenReturn(null);

        CollectionCSV colCsv = service.loadCollectionData(VALID_COLLECTION, LANGUAGE);
        assertNull("Null data expected.", colCsv);
    }

    /**
     * The service is used to load a particular collection that includes
     * the name of a book that does not exist in the archive. The resulting collection
     * CSV should exist (not be NULL), but have no rows.
     *
     * @throws IOException .
     */
    @Test
    public void loadCollectionDataWithInvalidBook() throws IOException {
        BookCollection col = loadValidCollection();
        when(mockStore.collection(anyString())).thenReturn(col);
        when(mockStore.book(anyString(), anyString())).thenReturn(store.loadBook(col, "INVALID_BOOK", null));

        CollectionCSV collectionCSV = service.loadCollectionData(VALID_COLLECTION, LANGUAGE);
        assertEquals("Unexpected ID found.", "valid", collectionCSV.getId());
        assertEquals("Unexpected number of rows found.", 0, collectionCSV.size());
    }

    /**
     * Assume the service is used to load the collection data for one particular
     * collection, which contains only a single book. The resulting CSV should
     * contain only a single row representing that book.
     *
     * @throws IOException .
     */
    @Test
    public void loadCollectionDataWhenCollectionHasOneBook() throws IOException {
        BookCollection mockCollection = new BookCollection();
        mockCollection.setId("collection");
        mockCollection.setBooks(new String[]{"book1"});

        // Load mockCollection when a collection is requested.
        when(mockStore.collection(anyString())).thenReturn(mockCollection);
        // Load LudwigXV7 from test data when a book is requested.
        when(mockStore.book(anyString(), anyString())).thenReturn(loadValidLudwigXV7());

        CollectionCSV collectionCSV = service.loadCollectionData("collection", LANGUAGE);

        assertNotNull("Collection CSV data is missing.", collectionCSV);
        assertEquals("Unexpected collection ID found.", "collection", collectionCSV.getId());
        assertEquals("Unexpected number of CSV rows found.", 1, collectionCSV.size());
    }

    /**
     * The service is used to load a particular collection that contains only
     * one book. This book is missing the 'BookTexts' in its metadata. The resulting
     * CSV should have one row.
     *
     * @throws IOException .
     */
    @Test
    public void loadCollectionDataWithBookMissingTexts() throws IOException {
        BookCollection mockCollection = new BookCollection();
        mockCollection.setId("collection");
        mockCollection.setBooks(new String[]{"book1"});

        // Load mockCollection when a collection is requested.
        when(mockStore.collection(anyString())).thenReturn(mockCollection);
        // Load LudwigXV7 from test data when a book is requested.
        Book book = loadValidLudwigXV7();

        // Simulate the book having no 'texts' or <msItem>
        BookMetadata md = book.getBookMetadata(LANGUAGE);
        md.setTexts(null);

        when(mockStore.book(anyString(), anyString())).thenReturn(book);

        CollectionCSV collectionCSV = service.loadCollectionData("collection", LANGUAGE);
        assertEquals("Unexpected ID found for the collection.", "collection", collectionCSV.getId());
        assertEquals("Unexpected number of rows found.", 1, collectionCSV.size());
    }

    /**
     * Load a collection that contains a book that has no metadata file associated
     * with it. The resulting CSV should exist with a row representing the book,
     * but the row will contain only the book's name.
     *
     * @throws IOException .
     */
    @Test
    public void loadCollectionDataWithMissingMetadata() throws IOException {
        BookCollection mockCollection = new BookCollection();
        mockCollection.setId("collection");
        mockCollection.setBooks(new String[]{"book1"});

        // Load mockCollection when a collection is requested.
        when(mockStore.collection(anyString())).thenReturn(mockCollection);
        // Load LudwigXV7 from test data when a book is requested.
        Book book = loadValidLudwigXV7();
        book.setBookMetadata(new HashMap<String, BookMetadata>());
        assertNull("Metadata should be NULL", book.getBookMetadata(LANGUAGE));

        when(mockStore.book(anyString(), anyString())).thenReturn(book);

        CollectionCSV collectionCSV = service.loadCollectionData("collection", LANGUAGE);
        assertNotNull("Collection ID missing.", collectionCSV.getId());
        assertEquals("Unexpected number of rows found.", 1, collectionCSV.size());

        CSVRow row = collectionCSV.getRow(0);
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
     * The service is used to load book data in a collection that does not
     * exist in the archive. This service call should return NULL without
     * throwing any exceptions.
     *
     * @throws IOException .
     */
    @Test
    public void loadCollectionBookDataWithBadCollection() throws IOException {
        when(mockStore.collection(anyString())).thenReturn(null);
        assertNull("NULL value expected.", service.loadCollectionBookData(VALID_COLLECTION, LANGUAGE));
    }

    /**
     * The service loads the book data for a collection that contains a
     * book that is not present in the archive. The CSV result should
     * exist, but the CSV row representing this bad book should not
     * exist. Since the bad book is the only book in this collection, the
     * CSV should have 0 rows.
     *
     * @throws IOException .
     */
    @Test
    public void loadCollectionBookDataWithBadBook() throws IOException {
        BookCollection mockCollection = new BookCollection();
        mockCollection.setId("COLLECTION");
        mockCollection.setBooks(new String[]{"BAD_NAME"});

        when(mockStore.collection(anyString())).thenReturn(mockCollection);
        when(mockStore.book(anyString(), anyString())).thenReturn(
                store.loadBook(mockCollection, "BAD_NAME", null));

        BookDataCSV csv = service.loadCollectionBookData("COLLECTION", LANGUAGE);
        assertEquals("Unexpected collection ID found.", "COLLECTION", csv.getId());
        assertEquals("Unexpected number of rows found.", 0, csv.size());
    }

    /**
     * Book selection data is loaded for some criteria for a collection that
     * does not exist in the archive. NULL should be returned with no error.
     *
     * @throws IOException .
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
     * @throws IOException .
     */
    @Test
    public void bookSelectionDataWithBadBook() throws IOException {
        BookCollection mockCollection = new BookCollection();
        mockCollection.setId("COLLECTION");
        mockCollection.setBooks(new String[]{"BAD_NAME"});

        when(mockStore.collection(anyString())).thenReturn(mockCollection);
        when(mockStore.book(anyString(), anyString())).thenReturn(
                store.loadBook(mockCollection, "BAD_NAME", null));

        for (SelectCategory category : SelectCategory.values()) {
            BookSelectList data = service.loadBookSelectionData("COLLECTION", category, LANGUAGE);
            assertNotNull("Result should not be NULL.", data);
            assertEquals("Unexpected collection name found.", "COLLECTION", data.getCollection());
            assertEquals("Unexpected book select category found.", category, data.getCategory());
            assertNotNull("Data list cannot be NULL.", data.asList());
            assertEquals("Unexpected number of rows found.", 0, data.asList().size());
        }
    }
}
