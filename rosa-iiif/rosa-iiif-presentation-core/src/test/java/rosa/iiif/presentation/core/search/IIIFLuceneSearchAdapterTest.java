package rosa.iiif.presentation.core.search;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import rosa.archive.core.ArchiveNameParser;
import rosa.archive.core.BaseArchiveTest;
import rosa.iiif.presentation.core.transform.impl.AnnotationTransformer;
import rosa.iiif.presentation.model.PresentationRequest;
import rosa.iiif.presentation.model.PresentationRequestType;
import rosa.iiif.presentation.model.search.IIIFSearchHit;
import rosa.iiif.presentation.model.search.IIIFSearchRequest;
import rosa.iiif.presentation.model.search.IIIFSearchResult;
import rosa.search.model.Query;
import rosa.search.model.QueryOperation;
import rosa.search.model.SearchFields;
import rosa.search.model.SearchMatch;
import rosa.search.model.SearchResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class IIIFLuceneSearchAdapterTest extends BaseArchiveTest {

    private IIIFLuceneSearchAdapter adapter;

    @Before
    public void setup() {
        String scheme = "http";
        String host = "serenity.dkc.jhu.edu";
        int port = 80;
        String pres_prefix = "/pres";

        IIIFSearchRequestFormatter requestFormatter = new IIIFSearchRequestFormatter(scheme, host, pres_prefix, port);

        this.adapter = new IIIFLuceneSearchAdapter(new AnnotationTransformer(requestFormatter, new ArchiveNameParser()), store,
                requestFormatter);
    }

    @Test
    public void blankIiifToLuceneQueryTest() {
        IIIFSearchRequest request = new IIIFSearchRequest(mockRequest(), "");
        Query result = adapter.iiifToLuceneQuery(request);

        assertNotNull("Resulting Query obj was NULL.", result);
        Assert.assertEquals("Unexpected Query found.", expectedBlankQuery(), result);
    }

    @Test
    public void iiifToLuceneQueryTest() {
        IIIFSearchRequest request = new IIIFSearchRequest(mockRequest(), "Moo cow");
        Query result = adapter.iiifToLuceneQuery(request);

        assertNotNull("Resulting Lucene query was NULL.", result);
        Assert.assertEquals("Unexpected result found", expectedQuery(), result);
    }

    @Test
    public void luceneResultToIIIFTest() throws Exception {
        IIIFSearchResult result = adapter.luceneResultToIIIF(mockSearchResult());

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
        List<String> testList = Arrays.asList(SearchFields.ANNOTATION_TEXT.name(), "asdf <B>fdsa</B> <B>fdas</B> asdf", SearchFields.ANNOTATION_TEXT.name(), "sfad <B>fdsa</B> JFIO <B>ifsa</B>");

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

        System.err.println(expected);
        System.err.println();
        
        List<IIIFSearchHit> hits = adapter.getContextHits(testList, "ID", "COLLECTION", "BOOK");
        
        System.err.println(hits);
        
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
                    Arrays.asList(SearchFields.ANNOTATION_TEXT.name(), "asdf <B>fdsa</B> asdf", SearchFields.ANNOTATION_TEXT.name(), "sfad <B>fdsa</B> JFIO ifsa I")
            ));
        }

        return new SearchResult(10L, 50L, matches.toArray(new SearchMatch[matches.size()]), "");
    }

    private Query expectedBlankQuery() {
        return new Query(
                QueryOperation.AND,
                new Query(SearchFields.IMAGE_NAME, "Bessy"),
                new Query(SearchFields.BOOK_ID, "cow"),
                new Query(SearchFields.COLLECTION_ID, "moo")
        );
    }

    private PresentationRequest mockRequest() {
        return new PresentationRequest("moo.cow", "Bessy", PresentationRequestType.CANVAS);
    }

    private Query expectedQuery() {
        return new Query(
                QueryOperation.AND,
                allQuery("Moo cow"),
                new Query(SearchFields.IMAGE_NAME, "Bessy"),
                new Query(SearchFields.BOOK_ID, "cow"),
                new Query(SearchFields.COLLECTION_ID, "moo")
        );
    }

    private Query allQuery(String term) {
        return new Query(
                QueryOperation.OR,
                new Query(SearchFields.ANNOTATION_TEXT, term)
        );
    }

}
