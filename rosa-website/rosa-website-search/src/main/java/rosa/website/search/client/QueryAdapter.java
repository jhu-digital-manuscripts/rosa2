package rosa.website.search.client;

import rosa.search.model.Query;

public interface QueryAdapter {
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
     * Get the search token from a search service query. This query uses terms
     * recognized by the search service that must be adapted into fields seen
     * in the user interface. The resulting token is usable in the history
     * fragment.
     *
     * @param query search service query
     * @return the search token
     */
    String toToken(Query query);
}
