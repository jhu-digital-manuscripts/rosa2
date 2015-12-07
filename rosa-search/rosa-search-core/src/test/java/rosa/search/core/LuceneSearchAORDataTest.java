package rosa.search.core;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import rosa.archive.core.BaseArchiveTest;
import rosa.search.model.Query;
import rosa.search.model.QueryOperation;
import rosa.search.model.SearchFields;
import rosa.search.model.SearchMatch;
import rosa.search.model.SearchResult;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LuceneSearchAORDataTest extends BaseArchiveTest {
    private LuceneSearchService service;

    @Rule
    public TemporaryFolder tmpfolder = new TemporaryFolder();

    @Before
    public void setupArchiveStore() throws Exception {
        super.setupArchiveStore();

        service = new LuceneSearchService(tmpfolder.newFolder().toPath());
        service.update(store, VALID_COLLECTION);
    }

    @After
    public void cleanup() {
        if (service != null) {
            service.shutdown();
        }
    }

    @Test
    public void testSearchAORSymbol() throws Exception {
        {
            Query query = new Query(SearchFields.AOR_SYMBOLS, "Sun");
            SearchResult result = service.search(query, null);

            assertNotNull("Search result was NULL", result);
            assertEquals("Unexpected number of results found.", 37, result.getTotal());
        }

        {
            Query query = new Query(SearchFields.AOR_SYMBOLS, "Mars");
            SearchResult result = service.search(query, null);

            assertNotNull("Search results was NULL.", result);
            assertEquals("Unexpected number of results found.", 2, result.getTotal());

            for (SearchMatch m : result.getMatches()) {
                assertTrue("Unexpected ID found.",
                        m.getId().endsWith("FolgersHa2.029r.tif") || m.getId().endsWith("FolgersHa2.026r.tif"));
            }
        }
    }

    @Test
    public void testSearchMarginaliaTranslation() throws Exception {
        Query query = new Query(SearchFields.AOR_MARGINALIA_TRANSLATIONS,
                "if you wish, you may command the citizens.");
        SearchResult result = service.search(query, null);

        assertNotNull("Search result was NULL.", result);
        assertEquals("Unexpected number of results found.", 1, result.getTotal());
        assertTrue("Unexpected result ID found.", result.getMatches()[0].getId().endsWith("FolgersHa2.036v.tif"));
    }

    @Test(expected = NullPointerException.class)
    public void testBlankQuery() throws Exception {
        service.search(new Query(), null);
    }

    @Test
    public void testNoSearchTermQuery() throws Exception {
        SearchResult result = service.search(new Query(QueryOperation.AND), null);
        assertNotNull("Result was NULL.", result);
        assertEquals("Unexpected number of results found.", 0, result.getTotal());
    }
}
