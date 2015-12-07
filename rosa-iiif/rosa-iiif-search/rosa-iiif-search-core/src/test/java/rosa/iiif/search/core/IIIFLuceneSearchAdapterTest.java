package rosa.iiif.search.core;

import org.junit.Before;
import org.junit.Test;
import rosa.iiif.search.model.IIIFSearchRequest;
import rosa.search.model.Query;
import rosa.search.model.QueryOperation;
import rosa.search.model.SearchFields;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class IIIFLuceneSearchAdapterTest {

    private IIIFLuceneSearchAdapter adapter;

    @Before
    public void setup() {
        this.adapter = new IIIFLuceneSearchAdapter();
    }

    @Test
    public void blankIiifToLuceneQueryTest() {
        IIIFSearchRequest request = new IIIFSearchRequest("");
        Query result = adapter.iiifToLuceneQuery(request);

        assertNotNull(result);
        System.out.println(result.toString());
    }

    @Test
    public void iiifToLuceneQueryTest() {
        IIIFSearchRequest request = new IIIFSearchRequest("Moo cow");
        Query result = adapter.iiifToLuceneQuery(request);

        assertNotNull("Resulting Lucene query was NULL.", result);
        assertEquals("Unexpected result found", expectedQuery(), result);
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
