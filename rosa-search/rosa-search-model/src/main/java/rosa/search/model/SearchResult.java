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
    private SearchMatch[] matches;
    private SortOrder sort_order;
    private String debug;

    public SearchResult() {
        this(0, 0, new SearchMatch[] {}, null, null);
    }

    public SearchResult(long offset, long total, SearchMatch[] matches,
            SortOrder sort_order) {
        this(offset, total, matches, sort_order, null);
    }
    
    public SearchResult(long offset, long total, SearchMatch[] matches,
            SortOrder sort_order, String debug) {
        this.offset = offset;
        this.total = total;
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

    @Override
    public String toString() {
        return "SearchResult [offset=" + offset + ", total=" + total
                + ", matches=" + Arrays.toString(matches) + ", sort_order="
                + sort_order +  ", debug=" + debug + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(matches);
        result = prime * result + (int) (offset ^ (offset >>> 32));
        result = prime * result
                + ((sort_order == null) ? 0 : sort_order.hashCode());
        result = prime * result
                + ((debug == null) ? 0 : debug.hashCode());        
        result = prime * result + (int) (total ^ (total >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof SearchResult))
            return false;
        SearchResult other = (SearchResult) obj;
        if (!Arrays.equals(matches, other.matches))
            return false;
        if (offset != other.offset)
            return false;
        if (sort_order != other.sort_order)
            return false;
        if (debug == null) {
            if (other.debug != null)
                return false;
        } else if (!debug.equals(other.debug))
            return false;        
        if (total != other.total)
            return false;
        return true;
    }
}
