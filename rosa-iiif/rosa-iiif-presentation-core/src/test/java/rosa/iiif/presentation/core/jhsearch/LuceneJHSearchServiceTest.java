package rosa.iiif.presentation.core.jhsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import rosa.archive.core.BaseArchiveTest;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
import rosa.iiif.presentation.model.PresentationRequest;
import rosa.iiif.presentation.model.PresentationRequestType;
import rosa.search.model.Query;
import rosa.search.model.QueryOperation;
import rosa.search.model.SearchResult;

public class LuceneJHSearchServiceTest extends BaseArchiveTest {
    private LuceneJHSearchService service;
    private IIIFPresentationRequestFormatter formatter;
    
    @Rule
    public TemporaryFolder tmpfolder = new TemporaryFolder();

    @Before
    public void setupArchiveStore() throws Exception {
        super.setupArchiveStore();

        String scheme = "http";
        String host = "serenity.dkc.jhu.edu";
        int port = 80;
        String pres_prefix = "/pres";

        formatter = new IIIFPresentationRequestFormatter(scheme, host, pres_prefix, port);
        
        service = new LuceneJHSearchService(tmpfolder.newFolder().toPath(), formatter);
        service.update(store, VALID_COLLECTION);
    }

    @After
    public void cleanup() {
        if (service != null) {
            service.shutdown();
        }
    }

    @Test
    public void testSearchSymbol() throws Exception {

        Query query = new Query(JHSearchFields.SYMBOL, "Sun");
        SearchResult result = service.search(query, null);

        assertNotNull("Search result was NULL", result);
        assertEquals("Unexpected number of results found.", 37, result.getTotal());
    }

    @Test
    public void testSearchMarginaliaTranslationPhrase() throws Exception {
        Query query = new Query(JHSearchFields.MARGINALIA, "\"if you wish, you may command the citizens.\"");
        SearchResult result = service.search(query, null);

        assertNotNull("Search result was NULL.", result);
        assertEquals("Unexpected number of results found.", 1, result.getTotal());
        assertTrue("Unexpected result ID found.", result.getMatches()[0].getId().contains("FolgersHa2.036v.tif"));
    }

    @Test
    public void testBlankQuery() throws Exception {
        SearchResult result = service.search(new Query(), null);
        assertNotNull("Result was NULL.", result);
        assertEquals("Unexpected number of results found.", 0, result.getTotal());
    }

    @Test
    public void testNoSearchTermQuery() throws Exception {
        SearchResult result = service.search(new Query(QueryOperation.AND), null);
        assertNotNull("Result was NULL.", result);
        assertEquals("Unexpected number of results found.", 0, result.getTotal());
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
}
