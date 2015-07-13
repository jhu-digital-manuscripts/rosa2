package rosa.website.search.client;

import rosa.search.model.Query;

import java.util.Map;

public interface QueryUtil {
    /**
     * Take a String search token that uses UI search categories and adapt
     * it into a Query that uses Lucene search fields. This query is capable
     * of being used in the {@link rosa.search.core.SearchService}
     *
     * @param token string token
     * @return the query
     */
    Query toQuery(String token);

//    /**
//     * Get the search token from a search service query. This query uses terms
//     * recognized by the search service that must be adapted into fields seen
//     * in the user interface. The resulting token is usable in the history
//     * fragment.
//     *
//     * @param query search service query
//     * @return the search token
//     */
//    String toToken(Query query);

    /**
     * Separate a string search token, that has been used as a history fragment,
     * into a map containing SearchCategories and search terms.
     *
     * Example token:
     *      ALL;1234;POETRY;qwer-;rewq;RUBRIC;asdf;ALL;lkhj;BOOK;Marne3,AssembleeNationale1230,CodGall80;0
     *
     * @param token string search token
     * @return map of SearchCategories -&gt; search term
     */
    Map<SearchCategory, String> queryParts(String token);

    /**
     * Get all of the books by which a search will be restricted from the search
     * token.
     *
     * @param token string search token
     * @return an array containing the book restriction list
     */
    String[] bookRestrictionList(String token);

    /**
     * Get the offset of the search results from the search token.
     *
     * @param token string search token
     * @return integer offset of the search results
     */
    int offset(String token);
}
