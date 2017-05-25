package rosa.search.model;

import java.io.Serializable;
import java.util.List;

/**
 * The search options are directions to the search service about how a query
 * should be executed.
 * 
 * The match count is the number of matches which the search service will
 * return.
 * 
 * The offset is the position of the first match which the search service should
 * return in the list of total results.
 */
public class SearchOptions implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_MATCH_COUNT = 30;

    private long offset;
    private int match_count;
    private SortOrder sort_order;
    private List<QueryTerm> categories;

    public SearchOptions() {
        this.offset = 0;
        this.match_count = DEFAULT_MATCH_COUNT;
        this.sort_order = SortOrder.RELEVANCE;
        this.categories = null;
    }

    public SearchOptions(long offset, int match_count) {
        this.offset = offset;
        this.match_count = match_count;
        this.sort_order = SortOrder.RELEVANCE;
        this.categories = null;
    }
    
    public SearchOptions(long offset, int match_count, SortOrder sort_order) {
        this.offset = offset;
        this.match_count = match_count;
        this.sort_order = sort_order;
        this.categories = null;
    }

    public SearchOptions(SearchOptions opts) {
        this.offset = opts.offset;
        this.match_count = opts.match_count;
        this.sort_order = opts.sort_order;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        if (offset < 0) {
            offset = 0;
        }

        this.offset = offset;
    }

    public int getMatchCount() {
        return match_count;
    }

    public void setMatchCount(int matches) {
        if (matches < 0) {
            matches = DEFAULT_MATCH_COUNT;
        }

        this.match_count = matches;
    }
    
    public SortOrder getSortOrder() {
    	return sort_order;
    }
    
    public void setSortOrder(SortOrder sort_order) {
    	this.sort_order = sort_order;
    }
    
    public List<QueryTerm> getCategories() {
        return categories;
    }

    public void setCategories(List<QueryTerm> categories) {
        this.categories = categories;
    }    

    @Override
    public String toString() {
        return "SearchOptions [offset=" + offset + ", match_count="
                + match_count + ", sort_order=" + sort_order + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + match_count;
        result = prime * result + + ((sort_order == null) ? 0 : sort_order.hashCode());
        result = prime * result + + ((categories == null) ? 0 : categories.hashCode());        
        result = prime * result + (int) (offset ^ (offset >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof SearchOptions))
            return false;
        SearchOptions other = (SearchOptions) obj;
        if (match_count != other.match_count)
            return false;
        if (sort_order != other.sort_order)
            return false;        
        if (categories != other.categories)
            return false;                
        if (offset != other.offset)
            return false;
        return true;
    }
}
