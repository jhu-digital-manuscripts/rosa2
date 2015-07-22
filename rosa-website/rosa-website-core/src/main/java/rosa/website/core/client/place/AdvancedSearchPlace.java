package rosa.website.core.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

/**
 * Represents a place to construct detailed search queries and display
 * search results of these queries, if the search token already exists.
 */
public class AdvancedSearchPlace extends Place {

    private final String searchToken;
    private final String resumeToken;

    /**
     * Create a new AdvancedSearchPlace with no search token.
     */
    public AdvancedSearchPlace() {
        this(null, null);
    }

    /**
     * Create a new AdvancedSearchPlace with a search token.
     *
     * @param searchToken .
     */
    public AdvancedSearchPlace(String searchToken) {
        this(searchToken, null);
    }

    public AdvancedSearchPlace(String searchToken, String resumeToken) {
        this.searchToken = searchToken;
        this.resumeToken = resumeToken;
    }

    public String getSearchToken() {
        return searchToken;
    }

    public String getResumeToken() {
        return resumeToken;
    }

    @Prefix("search")
    public static class Tokenizer implements PlaceTokenizer<AdvancedSearchPlace> {
        @Override
        public AdvancedSearchPlace getPlace(String token) {
            if (token == null || token.isEmpty()) {
                return new AdvancedSearchPlace();
            } else {
                return new AdvancedSearchPlace(token);
            }
        }

        @Override
        public String getToken(AdvancedSearchPlace place) {
            return place.getSearchToken();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AdvancedSearchPlace that = (AdvancedSearchPlace) o;

        if (searchToken != null ? !searchToken.equals(that.searchToken) : that.searchToken != null) return false;
        return !(resumeToken != null ? !resumeToken.equals(that.resumeToken) : that.resumeToken != null);

    }

    @Override
    public int hashCode() {
        int result = searchToken != null ? searchToken.hashCode() : 0;
        result = 31 * result + (resumeToken != null ? resumeToken.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "AdvancedSearchPlace{" +
                "searchToken='" + searchToken + '\'' +
                ", resumeToken='" + resumeToken + '\'' +
                '}';
    }
}
