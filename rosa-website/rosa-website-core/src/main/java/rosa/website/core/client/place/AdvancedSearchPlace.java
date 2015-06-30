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

    /**
     * Create a new AdvancedSearchPlace with no search token.
     */
    public AdvancedSearchPlace() {
        this.searchToken = null;
    }

    /**
     * Create a new AdvancedSearchPlace with a search token.
     *
     * @param searchToken .
     */
    public AdvancedSearchPlace(String searchToken) {
        this.searchToken = searchToken;
    }

    public String getSearchToken() {
        return searchToken;
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

        return !(searchToken != null ? !searchToken.equals(that.searchToken) : that.searchToken != null);

    }

    @Override
    public int hashCode() {
        return searchToken != null ? searchToken.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "AdvancedSearchPlace{" +
                "searchToken='" + searchToken + '\'' +
                '}';
    }
}
