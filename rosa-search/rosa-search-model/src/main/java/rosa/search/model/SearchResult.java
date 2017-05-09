package rosa.search.model;

import java.io.Serializable;
import java.util.Arrays;

/**
 * The sublist in the list of total results matching a query given some options.
 */
public class SearchResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private long offset;
    private long total;
    private long max_matches;
    private SearchMatch[] matches;
    private SortOrder sort_order;
    private String debug;

    public SearchResult() {
        this(0, 0, 0, new SearchMatch[] {}, null, null);
    }

    public SearchResult(long offset, long total, long max_matches, SearchMatch[] matches,
            SortOrder sort_order) {
        this(offset, total, max_matches, matches, sort_order, null);
    }
    
    public SearchResult(long offset, long total, long max_matches, SearchMatch[] matches,
            SortOrder sort_order, String debug) {
        this.offset = offset;
        this.total = total;
        this.max_matches = max_matches;
        this.matches = matches;
        this.sort_order = sort_order;
        this.debug = debug;
    }

    public long getOffset() {
        return offset;
    }

    public long getTotal() {
        return total;
    }

    public SearchMatch[] getMatches() {
        return matches;
    }
    
    public String getDebugMessage() {
        return debug;
    }
    
    public void setDebugMessage(String debug) {
        this.debug = debug;
    }

    public SortOrder getSortOrder() {
        return sort_order;
    }

    public long getMaxMatches() {
        return max_matches;
    }

    @Override
    public String toString() {
        return "SearchResult{" + "offset=" + offset + ", total=" + total + ", max_matches=" + max_matches +
                ", matches=" + Arrays.toString(matches) + ", sort_order=" + sort_order +
                ", debug='" + debug + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SearchResult)) return false;

        SearchResult that = (SearchResult) o;

        if (offset != that.offset) return false;
        if (total != that.total) return false;
        if (max_matches != that.max_matches) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(matches, that.matches)) return false;
        if (sort_order != that.sort_order) return false;
        return debug != null ? debug.equals(that.debug) : that.debug == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (offset ^ (offset >>> 32));
        result = 31 * result + (int) (total ^ (total >>> 32));
        result = 31 * result + (int) (max_matches ^ (max_matches >>> 32));
        result = 31 * result + Arrays.hashCode(matches);
        result = 31 * result + (sort_order != null ? sort_order.hashCode() : 0);
        result = 31 * result + (debug != null ? debug.hashCode() : 0);
        return result;
    }
}
