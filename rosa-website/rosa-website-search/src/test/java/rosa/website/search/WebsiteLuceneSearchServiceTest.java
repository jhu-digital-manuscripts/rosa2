package rosa.website.search;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import rosa.archive.core.BaseSearchTest;
import rosa.archive.core.Store;
import rosa.archive.core.serialize.TranscriptionXmlSerializer;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.search.core.LuceneSearchService;
import rosa.search.core.SearchUtil;
import rosa.search.model.Query;
import rosa.search.model.QueryOperation;
import rosa.search.model.SearchMatch;
import rosa.search.model.SearchOptions;
import rosa.search.model.SearchResult;
import rosa.website.search.client.model.WebsiteSearchFields;

/**
 * Evaluate service against test data from rosa-archive-core.
 */
public class WebsiteLuceneSearchServiceTest extends BaseSearchTest {
    private static LuceneSearchService service;
    private static WebsiteLuceneMapper mapper;

    @ClassRule
    public static TemporaryFolder tmpfolder = new TemporaryFolder();

    @BeforeClass
    public static void setup() throws Exception {
        mapper = new WebsiteLuceneMapper();
        service = new LuceneSearchService(tmpfolder.newFolder().toPath(), mapper);
        service.update(store, VALID_COLLECTION);
    }

    @AfterClass
    public static void cleanup() {
        if (service != null) {
            service.shutdown();
        }
    }

    /**
     * Index the valid collection and check that expected number of images and
     * books are indexed.
     */
    @Test
    @Ignore
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
    public void testSearchImageName() throws Exception {
        // 1r should match all 1r folios including frontmatter and endmatter
        SearchResult result = service.search(new Query(WebsiteSearchFields.IMAGE_NAME,
                "1r"), null);

        assertNotNull(result);
        assertEquals(6, result.getTotal());        
        assertEquals(0, result.getOffset());
        assertNull(result.getResumeToken());

        for (SearchMatch match: result.getMatches()) {
            String image = SearchUtil.getImageFromId(match.getId());

            assertTrue(image.endsWith(".01r.tif")
                    || image.endsWith(".001r.tif"));
        }

    }

    @Test
    public void testSearchResume() throws Exception {
        Query query = new Query(WebsiteSearchFields.BOOK_ID, VALID_BOOK_LUDWIGXV7);
        Book book = loadBook(VALID_COLLECTION, VALID_BOOK_LUDWIGXV7);
        int num_book_images = book.getImages().getImages().size();
        int total_matches = num_book_images + 1;

        SearchOptions opts = new SearchOptions();

        opts.setMatchCount(total_matches);
        SearchResult expected_result = service.search(query, opts);

        assertEquals(total_matches, expected_result.getTotal());
        assertEquals(total_matches, expected_result.getMatches().length);

        opts.setMatchCount(50);

        for (long offset = 0; offset < total_matches;) {
            SearchResult result = service.search(query, opts);
            assertEquals(total_matches, result.getTotal());

            int expected_num_matches = opts.getMatchCount();

            if (offset + opts.getMatchCount() > total_matches) {
                expected_num_matches = total_matches - (int) offset;
            }

            assertEquals(expected_num_matches, result.getMatches().length);
            assertEquals(offset, result.getOffset());

            SearchMatch[] expected_matches = Arrays.copyOfRange(
                    expected_result.getMatches(), (int) offset, (int) offset
                            + expected_num_matches);

            assertArrayEquals(expected_matches, result.getMatches());

            offset = opts.getOffset() + result.getMatches().length;

            if (offset == total_matches) {
                assertNull(result.getResumeToken());
            } else {
                assertNotNull(result.getResumeToken());
            }

            opts.setOffset(offset);
            opts.setResumeToken(result.getResumeToken());
        }
    }
    
    @Test
    public void testSearchResumeNoToken() throws Exception {
        Query query = new Query(WebsiteSearchFields.BOOK_ID, VALID_BOOK_LUDWIGXV7);
        Book book = loadBook(VALID_COLLECTION, VALID_BOOK_LUDWIGXV7);
        int num_book_images = book.getImages().getImages().size();
        int total_matches = num_book_images + 1;

        SearchOptions opts = new SearchOptions();

        opts.setMatchCount(total_matches);
        SearchResult expected_result = service.search(query, opts);

        assertEquals(total_matches, expected_result.getTotal());
        assertEquals(total_matches, expected_result.getMatches().length);

        opts.setMatchCount(50);

        for (long offset = 0; offset < total_matches;) {
            SearchResult result = service.search(query, opts);
            assertEquals(total_matches, result.getTotal());

            int expected_num_matches = opts.getMatchCount();

            if (offset + opts.getMatchCount() > total_matches) {
                expected_num_matches = total_matches - (int) offset;
            }

            assertEquals(expected_num_matches, result.getMatches().length);
            assertEquals(offset, result.getOffset());

            SearchMatch[] expected_matches = Arrays.copyOfRange(
                    expected_result.getMatches(), (int) offset, (int) offset
                            + expected_num_matches);

            assertArrayEquals(expected_matches, result.getMatches());

            offset = opts.getOffset() + result.getMatches().length;

            if (offset == total_matches) {
                assertNull(result.getResumeToken());
            } else {
                assertNotNull(result.getResumeToken());
            }

            opts.setOffset(offset);
            // Resume token not set
        }
    }

    /**
     * Ensure searching across English and French descriptions works.
     * 
     * @throws Exception
     */
    @Test
    public void testSearchDescription() throws Exception {
        // Search only matches English
        {
            SearchResult result = service.search(new Query(
                    WebsiteSearchFields.DESCRIPTION_TEXT, "morocco"), null);

            assertNotNull(result);
            assertEquals(1, result.getTotal());
            assertEquals(1, result.getMatches().length);
            assertEquals(0, result.getOffset());
            assertNull(result.getResumeToken());

            SearchMatch match = result.getMatches()[0];

            assertEquals(2, match.getContext().size());
            String field = match.getContext().get(0);
            String context = match.getContext().get(1);

            assertEquals(WebsiteSearchFields.DESCRIPTION_TEXT.getFieldName(), field);
            assertTrue(context.contains("morocco"));
        }

        // Search only matches French French
        {
            SearchResult result = service.search(new Query(
                    WebsiteSearchFields.DESCRIPTION_TEXT, "supprimée"), null);

            assertNotNull(result);
            assertEquals(1, result.getTotal());
            assertEquals(1, result.getMatches().length);
            assertEquals(0, result.getOffset());
            assertNull(result.getResumeToken());

            SearchMatch match = result.getMatches()[0];

            assertEquals(2, match.getContext().size());
            String field = match.getContext().get(0);
            String context = match.getContext().get(1);

            assertEquals(WebsiteSearchFields.DESCRIPTION_TEXT.getFieldName(), field);
            assertTrue(context.contains("supprimée"));
        }

    }

    @Test
    public void testSearchIllustrationChar() throws Exception {
        SearchResult result = service.search(new Query(QueryOperation.AND,
                new Query(WebsiteSearchFields.ILLUSTRATION_CHAR, "Faim"), new Query(
                        WebsiteSearchFields.BOOK_ID, VALID_BOOK_LUDWIGXV7)), null);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getMatches().length);
        assertEquals(0, result.getOffset());
        assertNull(result.getResumeToken());

        SearchMatch match = result.getMatches()[0];

        assertEquals(4, match.getContext().size());

        for (int i = 0; i < match.getContext().size();) {
            String field = match.getContext().get(i++);
            String context = match.getContext().get(i++);

            if (field.equals(WebsiteSearchFields.BOOK_ID.getFieldName())) {
                assertTrue(context.contains(VALID_BOOK_LUDWIGXV7));
            } else if (field.equals(WebsiteSearchFields.ILLUSTRATION_CHAR
                    .getFieldName())) {
                assertTrue(context.contains("Faim"));
            } else {
                assertTrue(false);
            }
        }
    }

    @Test
    public void testSearchNoMatches() throws Exception {
//        service.update(store, VALID_COLLECTION);

        SearchResult result = service.search(new Query(
                WebsiteSearchFields.COLLECTION_ID, "Moo"), null);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertEquals(0, result.getMatches().length);
        assertEquals(0, result.getOffset());
        assertNull(result.getResumeToken());
    }

    @Test
    @Ignore
    public void testClear() throws Exception {
        service.clear();
    }

    /**
     * Search for a specific string of text that appears in the transcription of
     * the text in LudwigXV7. This text appears on folio 013r only, so the
     * search service should return only a single match.
     *
     * @throws Exception
     */
    @Test
    public void testSearchTranscription() throws Exception {
//        service.update(store, VALID_COLLECTION);

        SearchResult result = service.search(
                new Query(WebsiteSearchFields.TRANSCRIPTION_TEXT, "\"Tout adés la ou il rendoit\""),
                null
        );

        assertNotNull(result);
        assertEquals("There should be only 1 match.", 1, result.getMatches().length);
        assertEquals("Unexpected search match ID found.",
                "valid;LudwigXV7;LudwigXV7.013r.tif", result.getMatches()[0].getId());
    }

    /**
     * Test a search on query that is a mix of String and Text queries.
     *
     * There are some search categories that a user can pick in the UI
     * that map to multiple Lucene search fields. In these cases, it
     * is possible that a Lucene search would consist of a combination
     * of phrase and exact searches.
     *
     * It MUST be the case that all fields can be searched at the same time,
     * without error. In the case of searching for a phrase over a simple
     * String search field, nothing should happen.
     *
     * @throws Exception
     */
    @Test
    public void testMixedQuerySearch() throws Exception {
//        service.update(store, VALID_COLLECTION);

        // Test and ID
        {
            Query query = new Query(
                    QueryOperation.OR,
                    new Query(WebsiteSearchFields.BOOK_ID, "LudwigXV7"),
                    new Query(WebsiteSearchFields.TRANSCRIPTION_TEXT, "LudwigXV7")
            );
            // Should match once for book, once for each image name
            SearchResult result = service.search(query, null);

            assertNotNull("No result found.", result);
            assertEquals("Unexpected number of results found.", 289, result.getTotal());
        }

        // Test a general phrase
        {
            Query query = new Query(
                    QueryOperation.OR,
                    new Query(WebsiteSearchFields.BOOK_ID, "\"Tout adés la ou il rendoit\""),
                    new Query(WebsiteSearchFields.TRANSCRIPTION_TEXT, "\"Tout adés la ou il rendoit\"")
            );
            SearchResult result = service.search(query, null);

            assertNotNull("No result found.", result);
            assertEquals("Unexpected number of results found.", 1, result.getTotal());
        }
    }

    @Ignore
    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateDouce() throws Exception {
        Store fakeStore = mock(Store.class);

        Book b = loadValidLudwigXV7();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("Douce195.transcription.xml")) {
            b.setTranscription(new TranscriptionXmlSerializer().read(in, null));
        }

        when(fakeStore.loadBookCollection(anyString(), anyList())).thenReturn(loadValidCollection());
        when(fakeStore.loadBook(any(BookCollection.class), anyString(), anyList())).thenReturn(b);

        service.update(fakeStore, VALID_COLLECTION);

    }

    @Ignore
    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateMorgan() throws Exception {
        Store fakeStore = mock(Store.class);

        Book b = loadValidLudwigXV7();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("Morgan948.transcription.xml")) {
            b.setTranscription(new TranscriptionXmlSerializer().read(in, null));
        }

        when(fakeStore.loadBookCollection(anyString(), anyList())).thenReturn(loadValidCollection());
        when(fakeStore.loadBook(any(BookCollection.class), anyString(), anyList())).thenReturn(b);

        service.update(fakeStore, VALID_COLLECTION);
    }

    @Ignore
    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateWalters() throws Exception {
        Store fakeStore = mock(Store.class);

        Book b = loadValidLudwigXV7();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("Walters143.transcription.xml")) {
            b.setTranscription(new TranscriptionXmlSerializer().read(in, null));
        }

        when(fakeStore.loadBookCollection(anyString(), anyList())).thenReturn(loadValidCollection());
        when(fakeStore.loadBook(any(BookCollection.class), anyString(), anyList())).thenReturn(b);

        service.update(fakeStore, VALID_COLLECTION);
    }

    @Test
    public void testNameVariant() throws Exception {
//        service.update(store, VALID_COLLECTION);

        testQueryVariants("L'Amans", "Lover");
        testQueryVariants("L'Amans", "Amant");
    }

    private void testQueryVariants(String reference, String variant) {
        org.apache.lucene.search.Query ref = mapper.createLuceneQuery(
                new Query(WebsiteSearchFields.ILLUSTRATION_CHAR, reference));
        org.apache.lucene.search.Query var = mapper.createLuceneQuery(
                new Query(WebsiteSearchFields.ILLUSTRATION_CHAR, variant));

        assertEquals("Queries should be equivalent", ref, var);
    }

}
