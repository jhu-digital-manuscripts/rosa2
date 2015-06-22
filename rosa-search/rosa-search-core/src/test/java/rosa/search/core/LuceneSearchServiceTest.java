package rosa.search.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import rosa.archive.core.BaseArchiveTest;
import rosa.archive.model.Book;
import rosa.search.model.Query;
import rosa.search.model.QueryOperation;
import rosa.search.model.SearchFields;
import rosa.search.model.SearchMatch;
import rosa.search.model.SearchOptions;
import rosa.search.model.SearchResult;

/**
 * Evaluate service against test data from rosa-archive-core.
 */
public class LuceneSearchServiceTest extends BaseArchiveTest {
    private LuceneSearchService service;

    @Rule
    public TemporaryFolder tmpfolder = new TemporaryFolder();

    @Before
    public void setupArchiveStore() throws Exception {
        super.setupArchiveStore();

        service = new LuceneSearchService(tmpfolder.newFolder().toPath());
    }

    @After
    public void cleanup() {
        if (service != null) {
            service.shutdown();
        }
    }

    /**
     * Index the valid collection and check that expected number of images and
     * books are indexed.
     */
    @Test
    public void testUpdateValidCollection() throws Exception {
        SearchResult result;

        // Check that the book is not indexed

        Query book_query = new Query(SearchFields.BOOK_ID, VALID_BOOK_LUDWIGXV7);

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

        result = service.search(new Query(SearchFields.COLLECTION_ID,
                VALID_COLLECTION), new SearchOptions());
        assertNotNull(result);

        Book book2 = loadBook(VALID_COLLECTION, VALID_BOOK_FOLGERSHA2);
        int num_book2_images = book2.getImages().getImages().size();

        assertEquals(2 + num_book1_images + num_book2_images, result.getTotal());
    }

    @Test
    public void testSearchImageName() throws Exception {
        service.update(store, VALID_COLLECTION);

        // 1r should match all 1r folios including frontmatter and endmatter
        SearchResult result = service.search(new Query(SearchFields.IMAGE_NAME,
                "1r"), null);

        assertNotNull(result);
        assertEquals(6, result.getTotal());
        assertEquals(6, result.getMatches().length);
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
        service.update(store, VALID_COLLECTION);

        Query query = new Query(SearchFields.BOOK_ID, VALID_BOOK_LUDWIGXV7);
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
        service.update(store, VALID_COLLECTION);

        Query query = new Query(SearchFields.BOOK_ID, VALID_BOOK_LUDWIGXV7);
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
        service.update(store, VALID_COLLECTION);

        // Search only matches English
        {
            SearchResult result = service.search(new Query(
                    SearchFields.DESCRIPTION_TEXT, "morocco"), null);

            assertNotNull(result);
            assertEquals(1, result.getTotal());
            assertEquals(1, result.getMatches().length);
            assertEquals(0, result.getOffset());
            assertNull(result.getResumeToken());

            SearchMatch match = result.getMatches()[0];

            assertEquals(2, match.getContext().size());
            String field = match.getContext().get(0);
            String context = match.getContext().get(1);

            assertEquals(SearchFields.DESCRIPTION_TEXT.getFieldName(), field);
            assertTrue(context.contains("morocco"));
        }

        // Search only matches French French
        {
            SearchResult result = service.search(new Query(
                    SearchFields.DESCRIPTION_TEXT, "supprimée"), null);

            assertNotNull(result);
            assertEquals(1, result.getTotal());
            assertEquals(1, result.getMatches().length);
            assertEquals(0, result.getOffset());
            assertNull(result.getResumeToken());

            SearchMatch match = result.getMatches()[0];

            assertEquals(2, match.getContext().size());
            String field = match.getContext().get(0);
            String context = match.getContext().get(1);

            assertEquals(SearchFields.DESCRIPTION_TEXT.getFieldName(), field);
            assertTrue(context.contains("supprimée"));
        }

    }

    @Test
    public void testSearchIllustrationChar() throws Exception {
        service.update(store, VALID_COLLECTION);

        SearchResult result = service.search(new Query(QueryOperation.AND,
                new Query(SearchFields.ILLUSTRATION_CHAR, "Faim"), new Query(
                        SearchFields.BOOK_ID, VALID_BOOK_LUDWIGXV7)), null);

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

            if (field.equals(SearchFields.BOOK_ID.getFieldName())) {
                assertTrue(context.contains(VALID_BOOK_LUDWIGXV7));
            } else if (field.equals(SearchFields.ILLUSTRATION_CHAR
                    .getFieldName())) {
                assertTrue(context.contains("Faim"));
            } else {
                assertTrue(false);
            }
        }
    }

    @Test
    public void testSearchNoMatches() throws Exception {
        service.update(store, VALID_COLLECTION);

        SearchResult result = service.search(new Query(
                SearchFields.COLLECTION_ID, "Moo"), null);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertEquals(0, result.getMatches().length);
        assertEquals(0, result.getOffset());
        assertNull(result.getResumeToken());
    }

    @Test
    public void testClear() throws Exception {
        service.clear();
    }

}
