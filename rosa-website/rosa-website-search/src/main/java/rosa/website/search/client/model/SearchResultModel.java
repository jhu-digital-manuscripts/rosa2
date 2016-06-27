package rosa.website.search.client.model;

import rosa.search.model.SearchResult;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Model object for search results UI. Wraps the {@link SearchResult} from the Lucene
 * search service.
 */
public class SearchResultModel implements Serializable {
    private static final long serialVersionUID = 1l;

    private SearchResult result;
    private List<SearchMatchModel> matches;

    /** No-arg constructor for GWT */
    @SuppressWarnings("unused")
    private SearchResultModel() {}

    public SearchResultModel(SearchResult result) {
        this.result = result;
        this.matches = new ArrayList<>();
    }

    public long getOffset() {
        return result.getOffset();
    }

    public long getTotal() {
        return result.getTotal();
    }

    public List<SearchMatchModel> getMatchList() {
        return matches;
    }

    public void addSearchMatch(SearchMatchModel match) {
        if (match != null) {
            this.matches.add(match);
        }
    }

    public void addSearchMatches(SearchMatchModel... matches) {
        if (matches != null) {
            this.matches.addAll(Arrays.asList(matches));
        }
    }

    public void clearMatches() {
        matches.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SearchResultModel that = (SearchResultModel) o;

        if (result != null ? !result.equals(that.result) : that.result != null) return false;
        return !(matches != null ? !matches.equals(that.matches) : that.matches != null);
    }

    @Override
    public int hashCode() {
        int result1 = super.hashCode();
        result1 = 31 * result1 + (result != null ? result.hashCode() : 0);
        result1 = 31 * result1 + (matches != null ? matches.hashCode() : 0);
        return result1;
    }

    @Override
    public String toString() {
        return "SearchResultModel{" + "result=" + result + ", matches=" + matches + '}';
    }
}
