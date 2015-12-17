package rosa.iiif.search.core;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import rosa.archive.core.ArchiveNameParser;
import rosa.archive.core.BaseArchiveTest;
import rosa.iiif.presentation.core.transform.impl.AnnotationTransformer;
import rosa.iiif.presentation.model.PresentationRequest;
import rosa.iiif.presentation.model.PresentationRequestType;
import rosa.iiif.presentation.model.annotation.Annotation;
import rosa.iiif.search.model.IIIFSearchHit;
import rosa.iiif.search.model.IIIFSearchRequest;
import rosa.iiif.search.model.IIIFSearchResult;
import rosa.search.core.LuceneSearchService;
import rosa.search.core.SearchService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RosaIIIFSearchServiceTest extends BaseArchiveTest {

    private RosaIIIFSearchService service;

    @Rule
    public TemporaryFolder tmpfolder = new TemporaryFolder();

    @Before
    public void setup() throws Exception {
        setupArchiveStore();

        SearchService luceneSearchService = new LuceneSearchService(tmpfolder.newFolder().toPath());
        luceneSearchService.update(store, VALID_COLLECTION);

        IIIFSearchRequestFormatter requestFormatter = new IIIFSearchRequestFormatter("SCHEME", "HOST", "/PREFIX", 80);
        IIIFLuceneSearchAdapter adapter = new IIIFLuceneSearchAdapter(
                new AnnotationTransformer(requestFormatter, new ArchiveNameParser()),
                store,
                requestFormatter
        );

        this.service = new RosaIIIFSearchService(store, luceneSearchService, adapter, "valid", requestFormatter);
    }

    /**
     * Search for the term "Sun" in the test data. There should be 44 instances found,
     * 40 of them from Symbols, 4 from Marginalia.
     *
     * @throws Exception .
     */
    @Test
    public void validCollectionSearchTest() throws Exception {
        IIIFSearchResult result = service.search(new IIIFSearchRequest(
                new PresentationRequest(null, VALID_COLLECTION, PresentationRequestType.COLLECTION), "Sun"));

        assertNotNull("Result is NULL.", result);
        assertEquals("Unexpected number of results found.", 44, result.getTotal());
        assertEquals("Unexpected number of Hits found.", 44, result.getHits().length);

        int marg_count = 0;
        int symbol_count = 0;
        for (IIIFSearchHit hit : result.getHits()) {
            if (hit.annotations[0].contains("marginalia")) {
                marg_count++;
            } else if (hit.annotations[0].contains("symbol")) {
                symbol_count++;
            }
        }

        assertEquals("Unexpected number of symbols found.", 40, symbol_count);
        assertEquals("Unexpected number of marginalia found.", 4, marg_count);
    }

    @Test
    public void validFolgersPage4rSunTest() throws Exception {
        IIIFSearchResult result = service.search(new IIIFSearchRequest(
                new PresentationRequest("valid.FolgersHa2", "FolgersHa2.004r.tif", PresentationRequestType.CANVAS), "sun"
        ));

        for (IIIFSearchHit hit : result.getHits()) {
            System.out.println(hit.annotations[0]);
        }

        System.out.println();

        for (Annotation ann : result.getAnnotations()) {
            System.out.println("  " + ann.getId());
            System.out.println("    " + ann.getDefaultTarget().getUri());
        }

        assertNotNull("Result is NULL", result);
        assertEquals("Unexpected number of results.", 4, result.getTotal());
    }

}
