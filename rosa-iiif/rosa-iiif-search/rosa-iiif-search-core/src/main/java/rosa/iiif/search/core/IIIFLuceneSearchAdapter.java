package rosa.iiif.search.core;

import rosa.iiif.search.model.IIIFSearchRequest;
import rosa.iiif.search.model.SearchCategory;
import rosa.search.model.Query;
import rosa.search.model.QueryOperation;
import rosa.search.model.SearchFields;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapt Lucene search results into JSON-LD that follows the IIIF Search API
 * (http://search.iiif.io/api/search/0.9/)
 *
 * Must be able to transform:
 *
 * - HTTP GET request formatted as specified in the IIIF Search API TO a Lucene
 * query that can be handed to the search service.
 *   - (http://search.iiif.io/api/search/0.9/#request)
 *
 * - Search results from the search service TO a format specified in the IIIF Search API
 *   - (http://search.iiif.io/api/search/0.9/#response)
 */
public class IIIFLuceneSearchAdapter {

    public IIIFLuceneSearchAdapter() {}

    /**
     * Transform a IIIF search request to a Lucene search query.
     *
     * @param iiifReq IIIF Search request
     * @return a Lucene query
     */
    public Query iiifToLuceneQuery(IIIFSearchRequest iiifReq) {
        // This will generate independent queries for each word...
        List<Query> top_query = new ArrayList<>();
        for (String queryTerm : iiifReq.queryTerms) {
            if (queryTerm == null || queryTerm.isEmpty()) {
                continue;
            }

            SearchCategory category = getSearchCategory(queryTerm);

            List<Query> children = new ArrayList<>();
            for (SearchFields luceneFields : category.luceneFields) {
                children.add(new Query(luceneFields, getSearchTerm(queryTerm)));
            }

            top_query.add(new Query(QueryOperation.OR, children.toArray(new Query[children.size()])));
        }

        /*
            Here, the rest of the parameters would be added to the query (motivation, date, user, box).

            In this specific case, motivation will be the same for all annotations that are being
            searched and will be omitted.

            The other parameters will not be searched for either, as no data is stored related
            to them. No user data, no time-stamp, location data is not precise enough for a box
            restriction.

            TODO to make this more general
            consider a generalized map: String (parameter) -> String[] (query terms)
            Parameters can be related (or the same as) Lucene fields. These can then be iterated
            over to generate the Lucene query.
         */




        return new Query(QueryOperation.AND, top_query.toArray(new Query[top_query.size()]));
    }



    /**
     * TODO for now, assume ALL category. this will change when faceted search is implemented
     *
     * @param queryFragment query fragment in question
     * @return search category associated with the fragment
     */
    private SearchCategory getSearchCategory(String queryFragment) {
        return SearchCategory.ALL;
    }

    /**
     * TODO for now, just return the fragment. this may change when faceted search is implemented
     * @param queryFrag part of query term from request
     * @return keyword to search for
     */
    private String getSearchTerm(String queryFrag) {
        return queryFrag;
    }
}
