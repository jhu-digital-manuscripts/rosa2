package rosa.iiif.search.core;

import org.junit.Before;
import org.junit.Test;
import rosa.archive.core.BaseArchiveTest;
import rosa.iiif.presentation.core.IIIFRequestFormatter;
import rosa.iiif.presentation.core.transform.impl.AnnotationTransformer;
import rosa.iiif.search.model.IIIFSearchHit;
import rosa.iiif.search.model.IIIFSearchRequest;
import rosa.iiif.search.model.IIIFSearchResult;
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

        IIIFRequestFormatter requestFormatter = new IIIFRequestFormatter(scheme, host, pres_prefix, port);

        this.adapter = new IIIFLuceneSearchAdapter(new AnnotationTransformer(requestFormatter), store,
                requestFormatter);
    }

    @Test
    public void blankIiifToLuceneQueryTest() {
        IIIFSearchRequest request = new IIIFSearchRequest("");
        Query result = adapter.iiifToLuceneQuery(request);

        assertNotNull("Resulting Query obj was NULL.", result);
        assertEquals("Unexpected Query found.", new Query(QueryOperation.AND), result);
    }

    @Test
    public void iiifToLuceneQueryTest() {
        IIIFSearchRequest request = new IIIFSearchRequest("Moo cow");
        Query result = adapter.iiifToLuceneQuery(request);

        assertNotNull("Resulting Lucene query was NULL.", result);
        assertEquals("Unexpected result found", expectedQuery(), result);
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
        List<String> testList = Arrays.asList("asdf <b>fdsa</b> <b>fdas</b> asdf", "sfad <b>fdsa</b> JFIO <b>ifsa</b>");

        /*
            IIIFSearchHit{annotations=[null], matching='fdsa fdas', before='asdf ', after=' asdf'},
            IIIFSearchHit{annotations=[null], matching='fdsa', before='sfad ', after=' JFIO '},
            IIIFSearchHit{annotations=[null], matching='ifsa', before=' JFIO ', after=''}
         */
        List<IIIFSearchHit> expected = Arrays.asList(
                new IIIFSearchHit(new String[] {"http://serenity.dkc.jhu.edu/pres/COLLECTION.BOOK/annotation/null"}, "fdsa fdas", "asdf ", " asdf"),
                new IIIFSearchHit(new String[] {"http://serenity.dkc.jhu.edu/pres/COLLECTION.BOOK/annotation/null"}, "fdsa", "sfad ", " JFIO "),
                new IIIFSearchHit(new String[] {"http://serenity.dkc.jhu.edu/pres/COLLECTION.BOOK/annotation/null"}, "ifsa", " JFIO ", "")
        );

        List<IIIFSearchHit> hits = adapter.getContextHits(testList, "ID", "COLLECTION", "BOOK");
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
                    Arrays.asList("asdf <b>fdsa</b> asdf", "sfad <b>fdsa</b> JFIO ifsa I")
            ));
        }

        return new SearchResult(10L, 50L, matches.toArray(new SearchMatch[matches.size()]), "");
    }

    private Query expectedQuery() {
        return new Query(
                QueryOperation.AND,
                allQuery("Moo"),
                allQuery("cow")
        );
    }

    private Query allQuery(String term) {
        return new Query(
                QueryOperation.OR,
                new Query(SearchFields.AOR_READER, term),
                new Query(SearchFields.AOR_PAGINATION, term),
                new Query(SearchFields.AOR_SIGNATURE, term),
                new Query(SearchFields.AOR_MARGINALIA_BOOKS, term),
                new Query(SearchFields.AOR_MARGINALIA_PEOPLE, term),
                new Query(SearchFields.AOR_MARGINALIA_LOCATIONS, term),
                new Query(SearchFields.AOR_MARGINALIA_TRANSCRIPTIONS, term),
                new Query(SearchFields.AOR_MARGINALIA_TRANSLATIONS, term),
                new Query(SearchFields.AOR_MARGINALIA_INTERNAL_REFS, term),
                new Query(SearchFields.AOR_MARKS, term),
                new Query(SearchFields.AOR_SYMBOLS, term),
                new Query(SearchFields.AOR_UNDERLINES, term),
                new Query(SearchFields.AOR_ERRATA, term),
                new Query(SearchFields.AOR_DRAWINGS, term),
                new Query(SearchFields.AOR_NUMERALS, term)
        );
    }

}
