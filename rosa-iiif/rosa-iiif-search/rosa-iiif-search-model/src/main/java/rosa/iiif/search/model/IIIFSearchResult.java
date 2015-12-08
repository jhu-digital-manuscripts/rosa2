package rosa.iiif.search.model;

import rosa.iiif.presentation.model.AnnotationList;
import rosa.iiif.presentation.model.IIIFNames;

import java.io.Serializable;
import java.util.Arrays;

/**
 * IIIF Search API v0.9
 *
 * http://search.iiif.io/api/search/0.9/#presentation-api-compatible-responses
 * http://search.iiif.io/api/search/0.9/#search-api-specific-responses
 */
public class IIIFSearchResult extends AnnotationList implements Serializable, IIIFSearchNames, IIIFNames {
    private static final long serialVersionUID = 1L;
    private static final String WITHIN_TYPE = "sc:Layer";

    /** Total number of results found. Useful with paging. */
    public int total;

    /**
     * Ignored parameters.
     *
     * IIIF Search requires parameter 'q'. 'motivation' is recommended and
     * three parameters 'date', 'user', 'box' are optional. Any ignored parameter
     * must be reported here.
     */
    public String[] ignored;

    /** List of 'hits' information containing context for results. */
    public IIIFSearchHit[] hits;

    // Info for paging
    /** Index of the first result in this page within the total */
    public int startIndex;

    // URIs for navigating results paging
    /** URI of next page of results */
    public String next;
    /** URI of previous page of results */
    public String prev;
    /** URI of first results page */
    public String first;
    /** URI of last results page */
    public String last;

    public IIIFSearchResult() {
        super();
        setContext(SEARCH_CONTEXT);
    }

    @Override
    public boolean canEqual(Object o) {
        return o instanceof IIIFSearchResult;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        IIIFSearchResult that = (IIIFSearchResult) o;

        if (!that.canEqual(this)) {
            return false;
        }

        if (total != that.total) return false;
        if (startIndex != that.startIndex) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(ignored, that.ignored)) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(hits, that.hits)) return false;
        if (next != null ? !next.equals(that.next) : that.next != null) return false;
        if (prev != null ? !prev.equals(that.prev) : that.prev != null) return false;
        if (first != null ? !first.equals(that.first) : that.first != null) return false;
        return !(last != null ? !last.equals(that.last) : that.last != null);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + total;
        result = 31 * result + (ignored != null ? Arrays.hashCode(ignored) : 0);
        result = 31 * result + (hits != null ? Arrays.hashCode(hits) : 0);
        result = 31 * result + startIndex;
        result = 31 * result + (next != null ? next.hashCode() : 0);
        result = 31 * result + (prev != null ? prev.hashCode() : 0);
        result = 31 * result + (first != null ? first.hashCode() : 0);
        result = 31 * result + (last != null ? last.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "IIIFSearchResult{" +
                "total=" + total +
                ", ignored=" + Arrays.toString(ignored) +
                ", hits=" + Arrays.toString(hits) +
                ", startIndex=" + startIndex +
                ", next='" + next + '\'' +
                ", prev='" + prev + '\'' +
                ", first='" + first + '\'' +
                ", last='" + last + '\'' +
                '}';
    }
}
