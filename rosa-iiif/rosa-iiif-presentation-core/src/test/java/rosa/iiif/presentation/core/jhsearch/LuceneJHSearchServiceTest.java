package rosa.iiif.presentation.core.jhsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import rosa.archive.core.BaseArchiveTest;
import rosa.archive.core.Store;
import rosa.archive.core.StoreImpl;
import rosa.archive.model.BookCollection;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
import rosa.iiif.presentation.model.PresentationRequest;
import rosa.iiif.presentation.model.PresentationRequestType;
import rosa.search.model.Query;
import rosa.search.model.QueryOperation;
import rosa.search.model.SearchResult;

public class LuceneJHSearchServiceTest extends BaseArchiveTest {
    private LuceneJHSearchService service;

    @Rule
    public TemporaryFolder tmpfolder = new TemporaryFolder();

    @Before
    public void setupArchiveStore() throws Exception {
        super.setupArchiveStore();

        String scheme = "http";
        String host = "serenity.dkc.jhu.edu";
        int port = 80;
        String pres_prefix = "/pres";

        service = new LuceneJHSearchService(tmpfolder.newFolder().toPath(),
                new IIIFPresentationRequestFormatter(scheme, host, pres_prefix, port));
        service.update(store, VALID_COLLECTION);
    }
    
    @After
    public void cleanup() {
        if (service != null) {
            service.shutdown();
        }
    }

    @Test
    public void testSearchSymbolSun() throws Exception {

        Query query = new Query(JHSearchFields.SYMBOL, "Sun");
        SearchResult result = service.search(query, null);

        assertNotNull("Search result was NULL", result);
        assertEquals("Unexpected number of results found.", 37, result.getTotal());
    }

    @Test
    public void testSearchSymbolMars() throws Exception {

        Query query = new Query(JHSearchFields.SYMBOL, "Mars");
        SearchResult result = service.search(query, null);

        assertNotNull("Search result was NULL", result);
        assertEquals("Unexpected number of results found.", 2, result.getTotal());
    }

    @Test
    public void testSearchSymbolAndMarginalia() throws Exception {
        Query query = new Query(QueryOperation.AND, new Query(JHSearchFields.SYMBOL, "Mars"),
                new Query(JHSearchFields.MARGINALIA, "Homer"));
        SearchResult result = service.search(query, null);

        assertNotNull("Search result was NULL", result);
        assertEquals("Unexpected number of results found.", 1, result.getTotal());
    }
    
    @Test
    public void testStemLatin() throws Exception {
        Query query = new Query(JHSearchFields.MARGINALIA, "maximus");
        SearchResult result = service.search(query, null);

        assertNotNull("Search result was NULL", result);
        
        assertEquals("Unexpected number of results found.", 9, result.getTotal());
    }

    @Test
    public void testSearchMarginaliaTranslationPhrase() throws Exception {
        Query query = new Query(JHSearchFields.MARGINALIA, "\"if you wish, you may command the citizens.\"");
        SearchResult result = service.search(query, null);

        assertNotNull("Search result was NULL.", result);
        assertEquals("Unexpected number of results found.", 1, result.getTotal());
        assertTrue("Unexpected result ID found.", result.getMatches()[0].getId().contains("36v"));
    }
    
    @Test
    public void testSearchMarginaliaAnchorText() throws Exception {
        Query query = new Query(JHSearchFields.MARGINALIA, "Giudeo");
        SearchResult result = service.search(query, null);

        assertNotNull("Search result was NULL.", result);
        assertEquals("Unexpected number of results found.", 1, result.getTotal());
        assertTrue("Unexpected result ID found.", result.getMatches()[0].getId().contains("7r"));
    }
    
    @Test
    public void testSearchMarginaliaXRef() throws Exception {
        Query query = new Query(JHSearchFields.CROSS_REFERENCE, "Erasmus");
        SearchResult result = service.search(query, null);

        assertNotNull("Search result was NULL.", result);
        assertEquals("Unexpected number of results found.", 1, result.getTotal());
        assertTrue("Unexpected result ID found.", result.getMatches()[0].getId().contains("1v"));
    }
    
    @Test
    public void testSearchMarginaliaEmphasis() throws Exception {
        Query query = new Query(JHSearchFields.EMPHASIS, "Bodini");
        SearchResult result = service.search(query, null);

        assertNotNull("Search result was NULL.", result);
        assertEquals("Unexpected number of results found.", 1, result.getTotal());
        assertTrue("Unexpected result ID found.", result.getMatches()[0].getId().contains("1r"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBlankQuery() throws Exception {
        service.search(new Query(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoSearchTermQuery() throws Exception {
        service.search(new Query(QueryOperation.AND), null);
    }

    @Test
    public void testHandleRequestSymbol() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PresentationRequest req = new PresentationRequest("valid.FolgersHa2", null, PresentationRequestType.MANIFEST);

        String query = "symbol:'Sun'";

        service.handle_request(req, query, 0, os);

        String result_json = os.toString("UTF-8");

        // TODO More full checks by actually parsing json structure
        assertTrue(result_json.contains("\"total\":37"));
        assertTrue(result_json.contains("\"context\":"));
        assertTrue(result_json.contains("\"manifest\":"));
        assertTrue(result_json.contains("\"object\":"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnknownField() throws Exception {
        service.search(new Query("unknown", "moo"), null);
    }

    /**
     * Search for a person's name that appears once in the Folgers book. Then search for
     * an alternate spelling of the name. Both should return a single result of the same
     * page.
     *
     * The result Strings will NOT be exactly the same. While the object/manifest will be
     * the same, the context will be different.
     *
     * @throws Exception
     */
    @Test
    public void testPersonSpellingVariant() throws Exception {
        PresentationRequest req = new PresentationRequest("valid.FolgersHa2", null, PresentationRequestType.MANIFEST);

        // Check this name: Baldo degli Ubaldi,Baldus,Baldus de Ubaldis,,
        // Alternate spelling
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            String query = "marginalia:'\"Baldus de Ubaldis\"'";

            service.handle_request(req, query, 0, out);

            String result = out.toString();
            assertNotNull(result);
            assertFalse("Result was empty.", result.isEmpty());
            assertTrue("Unexpected number of matches returned.", result.contains("\"total\":1"));
            assertTrue("Unexpected page label found.", result.contains("\"label\":\"25v\""));
            assertTrue("Unexpected page ID found.", result.contains(
                    "\"@id\":\"http://serenity.dkc.jhu.edu/pres/valid.FolgersHa2/canvas/25v\""
            ));
        }

        // Original spelling
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            String query = "marginalia:'\"Baldo degli Ubaldi\"'";

            service.handle_request(req, query, 0, out);

            String result = out.toString();
            assertNotNull(result);
            assertFalse("Result was empty.", result.isEmpty());
            assertTrue("Unexpected number of matches returned.", result.contains("\"total\":1"));
            assertTrue("Unexpected page label found.", result.contains("\"label\":\"25v\""));
            assertTrue("Unexpected page ID found.", result.contains(
                    "\"@id\":\"http://serenity.dkc.jhu.edu/pres/valid.FolgersHa2/canvas/25v\""
            ));
        }
    }

    /**
     * Search for a person's name that appears once in the Folgers book. Then search for
     * an alternate spelling of the name. The reference sheets containing spelling
     * variants is missing. While the alternate spelling search will not fail, it will
     * return zero results.
     *
     * @throws Exception
     */
    @Test
    public void testSpellingVariantWithNullReference() throws Exception {
        BookCollection mockCollection = loadValidCollection();
        mockCollection.setBooksRef(null);
        mockCollection.setPeopleRef(null);
        mockCollection.setLocationsRef(null);

        // The Store will return a BookCollection that is missing all spelling reference sheets,
        // Everything else will be the same as the real Store.
        Store mockStore = mock(StoreImpl.class);
        when(mockStore.loadBookCollection(anyString(), anyListOf(String.class))).thenReturn(mockCollection);
        when(mockStore.loadBook(any(BookCollection.class), eq("FolgersHa2"), anyListOf(String.class)))
                .thenReturn(loadValidFolgersHa2());
        when(mockStore.loadBook(any(BookCollection.class), eq("LudwigXV7"), anyListOf(String.class)))
                .thenReturn(loadValidLudwigXV7());

        // Clear search index and re-index with mocked data
        service.clear();
        service.update(mockStore, mockCollection.getId());

        PresentationRequest req = new PresentationRequest("valid.FolgersHa2", null, PresentationRequestType.MANIFEST);
        // Check this name: Baldo degli Ubaldi,Baldus,Baldus de Ubaldis,,
        // Alternate spelling
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            String query = "marginalia:'\"Baldus de Ubaldis\"'";

            service.handle_request(req, query, 0, out);

            String result = out.toString();
            assertNotNull(result);
            assertFalse("Result was empty.", result.isEmpty());
            assertTrue("Unexpected number of matches returned.", result.contains("\"total\":0"));
        }

        // Original spelling
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            String query = "marginalia:'\"Baldo degli Ubaldi\"'";

            service.handle_request(req, query, 0, out);

            String result = out.toString();
            assertNotNull(result);
            assertFalse("Result was empty.", result.isEmpty());
            assertTrue("Unexpected number of matches returned.", result.contains("\"total\":1"));
            assertTrue("Unexpected page label found.", result.contains("\"label\":\"25v\""));
            assertTrue("Unexpected page ID found.", result.contains(
                    "\"@id\":\"http://serenity.dkc.jhu.edu/pres/valid.FolgersHa2/canvas/25v\""
            ));
        }

    }
}
