package rosa.iiif.presentation.core.search;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import rosa.archive.core.BaseArchiveTest;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
import rosa.iiif.presentation.model.PresentationRequest;
import rosa.iiif.presentation.model.PresentationRequestType;
import rosa.iiif.presentation.model.search.IIIFSearchHit;
import rosa.iiif.presentation.model.search.IIIFSearchRequest;
import rosa.iiif.presentation.model.search.IIIFSearchResult;
import rosa.search.model.Query;
import rosa.search.model.QueryOperation;
import rosa.search.model.SearchMatch;
import rosa.search.model.SearchResult;


public class LuceneIIIFSearchServiceTest extends BaseArchiveTest {

    private LuceneIIIFSearchService service;

    @Rule
    public TemporaryFolder tmpfolder = new TemporaryFolder();

    @Before
    public void setup() throws Exception {
        setupArchiveStore();

        String scheme = "http";
        String host = "serenity.dkc.jhu.edu";
        int port = 80;
        String pres_prefix = "/pres";

        IIIFPresentationRequestFormatter requestFormatter = new IIIFPresentationRequestFormatter(scheme, host, pres_prefix, port);

        service = new LuceneIIIFSearchService(tmpfolder.newFolder().toPath(), requestFormatter);
        service.update(store, VALID_COLLECTION);
    }

    /**
     * Search for the term "Sun" in the test data. There should be 44 instances found,
     * 40 of them from Symbols, 4 from Marginalia.
     *
     * @throws Exception
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

        assertNotNull("Result is NULL", result);
        assertEquals("Unexpected number of results.", 4, result.getTotal());
    }
    
    
    // TODO redo these tests.
    
    @Test
    public void blankIiifToLuceneQueryTest() {
        IIIFSearchRequest request = new IIIFSearchRequest(mockRequest(), "");
        Query result = service.iiifToLuceneQuery(request);

        assertNotNull("Resulting Query obj was NULL.", result);
        Assert.assertEquals("Unexpected Query found.", expectedBlankQuery(), result);
    }

    @Test
    public void iiifToLuceneQueryTest() {
        IIIFSearchRequest request = new IIIFSearchRequest(mockRequest(), "Moo cow");
        Query result = service.iiifToLuceneQuery(request);

        assertNotNull("Resulting Lucene query was NULL.", result);
        Assert.assertEquals("Unexpected result found", expectedQuery(), result);
    }

    @Test
    public void luceneResultToIIIFTest() throws Exception {
        IIIFSearchResult result = service.luceneResultToIIIF(mockSearchResult());

        List<IIIFSearchHit> expected = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            String[] ID = new String[] {"http://serenity.dkc.jhu.edu/pres/valid.FolgersHa2/annotation/FolgersHa2.009r.tif_symbol_"+i};
            expected.add(new IIIFSearchHit(ID, "fdsa", "asdf ", " asdf"));
            expected.add(new IIIFSearchHit(ID, "fdsa", "sfad ", " JFIO ifsa I"));
        }

        assertNotNull("Result was NULL", result);
        assertArrayEquals("Unexpected list of hits found.", expected.toArray(new IIIFSearchHit[8]), result.getHits());
    }

    @Test
    public void getContextHitsTest() {
        List<String> testList = Arrays.asList(IIIFSearchFields.TEXT.name(), "asdf <B>fdsa</B> <B>fdas</B> asdf", IIIFSearchFields.TEXT.name(), "sfad <B>fdsa</B> JFIO <B>ifsa</B>");

        /*
            IIIFSearchHit{annotations=[null], matching='fdsa fdas', before='asdf ', after=' asdf'},
            IIIFSearchHit{annotations=[null], matching='fdsa', before='sfad ', after=' JFIO '},
            IIIFSearchHit{annotations=[null], matching='ifsa', before=' JFIO ', after=''}
         */
        List<IIIFSearchHit> expected = Arrays.asList(
                new IIIFSearchHit(new String[] {"http://serenity.dkc.jhu.edu/pres/COLLECTION.BOOK/annotation/null"}, "fdsa", "asdf ", " "),
                new IIIFSearchHit(new String[] {"http://serenity.dkc.jhu.edu/pres/COLLECTION.BOOK/annotation/null"}, "fdas", " ", " asdf"),
                new IIIFSearchHit(new String[] {"http://serenity.dkc.jhu.edu/pres/COLLECTION.BOOK/annotation/null"}, "fdsa", "sfad ", " JFIO "),
                new IIIFSearchHit(new String[] {"http://serenity.dkc.jhu.edu/pres/COLLECTION.BOOK/annotation/null"}, "ifsa", " JFIO ", "")
        );

        List<IIIFSearchHit> hits = service.getContextHits(testList, "ID", "COLLECTION", "BOOK");
        
        assertNotNull("Hits is NULL.", hits);
        assertFalse("Hits is empty/contains no hits.", hits.isEmpty());
        assertEquals("Unexpected list of IIIFSearchHits found.", expected, hits);
    }


    /**
     * Create fake search results based on page: FolgersHa2.009r.tif
     *
     * This page has 4 symbols.
     *
     * @return faked search results
     */
    private SearchResult mockSearchResult() {
        List<SearchMatch> matches = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            matches.add(new SearchMatch(
                    "valid;FolgersHa2;FolgersHa2.009r.tif;FolgersHa2.009r.tif_symbol_" + i,
                    Arrays.asList(IIIFSearchFields.TEXT.name(), "asdf <B>fdsa</B> asdf", IIIFSearchFields.TEXT.name(), "sfad <B>fdsa</B> JFIO ifsa I")
            ));
        }

        return new SearchResult(10L, 50L, matches.toArray(new SearchMatch[matches.size()]), "");
    }

    private Query expectedBlankQuery() {
        return new Query(
                QueryOperation.AND,
                new Query(IIIFSearchFields.IMAGE, "Bessy"),
                new Query(IIIFSearchFields.BOOK, "cow"),
                new Query(IIIFSearchFields.COLLECTION, "moo")
        );
    }

    private PresentationRequest mockRequest() {
        return new PresentationRequest("moo.cow", "Bessy", PresentationRequestType.CANVAS);
    }

    private Query expectedQuery() {
        return new Query(
                QueryOperation.AND,
                allQuery("Moo cow"),
                new Query(IIIFSearchFields.IMAGE, "Bessy"),
                new Query(IIIFSearchFields.BOOK, "cow"),
                new Query(IIIFSearchFields.COLLECTION, "moo")
        );
    }

    private Query allQuery(String term) {
        return new Query(
                QueryOperation.OR,
                new Query(IIIFSearchFields.TEXT, term)
        );
    }
    

}
