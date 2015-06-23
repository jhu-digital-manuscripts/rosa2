package rosa.search.model;

import java.io.Serializable;

/**
 * The search options are directions to the search service about how a query
 * should be executed.
 * 
 * The match count is the number of matches which the search service will
 * return.
 * 
 * The offset is the position of the first match which the search service should
 * return in the list of total results.
 * 
 * The resume token is used to efficiently resume a search. Using a resume token
 * is more efficient than an offset.
 */
public class SearchOptions implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_MAX_MATCHES = 30;

    private long offset;
    private int match_count;
    private String resume_token;

    public SearchOptions() {
        this.offset = 0;
        this.match_count = DEFAULT_MAX_MATCHES;
    }

    public SearchOptions(long offset, int match_count, String resume_token) {
        this.offset = offset;
        this.match_count = match_count;
        this.resume_token = resume_token;
    }

    public SearchOptions(SearchOptions opts) {
        this.offset = opts.offset;
        this.match_count = opts.match_count;
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
            matches = 0;
        }

        this.match_count = matches;
    }

    public String getResumeToken() {
        return resume_token;
    }

    public void setResumeToken(String resume_token) {
        this.resume_token = resume_token;
    }

    @Override
    public String toString() {
        return "SearchOptions [offset=" + offset + ", match_count="
                + match_count + ", resume_token=" + resume_token + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + match_count;
        result = prime * result + (int) (offset ^ (offset >>> 32));
        result = prime * result
                + ((resume_token == null) ? 0 : resume_token.hashCode());
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
        if (offset != other.offset)
            return false;
        if (resume_token == null) {
            if (other.resume_token != null)
                return false;
        } else if (!resume_token.equals(other.resume_token))
            return false;
        return true;
    }
}
