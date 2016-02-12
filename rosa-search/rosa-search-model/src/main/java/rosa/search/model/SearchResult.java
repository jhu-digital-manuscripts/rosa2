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
    private String resume_token;
    private String debug;

    public SearchResult() {
        this(0, 0, new SearchMatch[] {}, null, null);
    }

    public SearchResult(long offset, long total, SearchMatch[] matches,
            String resume_token) {
        this(offset, total, matches, resume_token, null);
    }
    
    public SearchResult(long offset, long total, SearchMatch[] matches,
            String resume_token, String debug) {
        this.offset = offset;
        this.total = total;
        this.matches = matches;
        this.resume_token = resume_token;
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

    /**
     * @return token used to resume the search after the last match returned
     */
    public String getResumeToken() {
        return resume_token;
    }

    @Override
    public String toString() {
        return "SearchResult [offset=" + offset + ", total=" + total
                + ", matches=" + Arrays.toString(matches) + ", resume_token="
                + resume_token +  ", debug=" + debug + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(matches);
        result = prime * result + (int) (offset ^ (offset >>> 32));
        result = prime * result
                + ((resume_token == null) ? 0 : resume_token.hashCode());
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
        if (resume_token == null) {
            if (other.resume_token != null)
                return false;
        } else if (!resume_token.equals(other.resume_token))
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
