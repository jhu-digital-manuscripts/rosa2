package rosa.website.search;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import rosa.archive.core.BaseArchiveTest;
import rosa.archive.core.Store;
import rosa.archive.core.serialize.TranscriptionXmlSerializer;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.search.core.LuceneSearchService;
import rosa.search.model.Query;
import rosa.search.model.SearchOptions;
import rosa.search.model.SearchResult;
import rosa.website.search.client.model.WebsiteSearchFields;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests here will change either the data that is indexed or the search index,
 * so cannot use {@link rosa.archive.core.BaseSearchTest} as they would interfere
 * with other tests. Each method will create/destroy its own test index.
 */
public class WebsiteSearchStatefulTest extends BaseArchiveTest {
    private LuceneSearchService service;

    @Rule
    public TemporaryFolder tmp_index_dir = new TemporaryFolder();

    /*
     * NOTE: According to JUnit4 documentation, @Before methods in superclasses are
     * run before those in subclasses.
     */
    @Before
    public void setup() throws Exception {
        service = new LuceneSearchService(tmp_index_dir.newFolder().toPath(), new WebsiteLuceneMapper());
    }

    /**
     * Index the valid collection and check that expected number of images and
     * books are indexed.
     */
    @Test
    public void testUpdateValidCollection() throws Exception {
        SearchResult result;

        // Check that the book is not indexed

        Query book_query = new Query(WebsiteSearchFields.BOOK_ID, VALID_BOOK_LUDWIGXV7);

        result = service.search(book_query, null);
        assertNotNull(result);

        assertEquals(0, result.getTotal());

        // Index the collection

        service.update(store, VALID_COLLECTION);

        // Confirm expected counts

        result = service.search(book_query, null);
        assertNotNull(result);

        Book book1 = loadBook(VALID_COLLECTION, VALID_BOOK_LUDWIGXV7);

        int num_book1_images = book1.getImages().getImages().size();

        assertEquals(num_book1_images + 1, result.getTotal());

        result = service.search(new Query(WebsiteSearchFields.COLLECTION_ID,
                VALID_COLLECTION), new SearchOptions());
        assertNotNull(result);

        Book book2 = loadBook(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2);

        int num_book2_images = book2.getImages().getImages().size();

        assertEquals(2 + num_book1_images + num_book2_images, result.getTotal());
    }

    @Test
    public void testClear() throws Exception {
        service.clear();
    }

    /*
     * Following three tests update the search index with several transcription
     * files. Each of these transcriptions have slightly different structure
     * (because TEI is so flexible).
     * The tests make sure that these different structures are indexed correctly.
     */

    @Test
    public void testUpdateDifferentTranscriptions() throws Exception {
        updateWithDifferentTranscription("Douce195.transcription.xml");
        updateWithDifferentTranscription("Morgan948.transcription.xml");
        updateWithDifferentTranscription("Walters143.transcription.xml");
    }

    @SuppressWarnings("unchecked")
    private void updateWithDifferentTranscription(String transcriptionFile) throws Exception {
        Store fakeStore = mock(Store.class);

        Book b = loadValidLudwigXV7();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(transcriptionFile)) {
            b.setTranscription(new TranscriptionXmlSerializer().read(in, null));
        }

        when(fakeStore.loadBookCollection(anyString(), anyList())).thenReturn(loadValidCollection());
        when(fakeStore.loadBook(any(BookCollection.class), anyString(), anyList())).thenReturn(b);

        service.update(fakeStore, VALID_COLLECTION);
    }

}
