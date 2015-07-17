package rosa.website.search.client;

import rosa.search.model.Query;
import rosa.search.model.QueryTerm;
import rosa.search.model.SearchMatch;

import java.util.List;

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

    /**
     * Separate a string search token, that has been used as a history fragment,
     * into a list of query terms, each containing the search category and search
     * term.
     *
     * Example token:
     *      ALL;1234;POETRY;qwer-;rewq;RUBRIC;asdf;ALL;lkhj;BOOK;Marne3,AssembleeNationale1230,CodGall80;0
     *
     * @param token string search token
     * @return list of search terms
     */
    List<QueryTerm> queryParts(String token);

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

    String getPageID(SearchMatch match);

    String getCollectionID(SearchMatch match);

    String getBookID(SearchMatch match);
}
