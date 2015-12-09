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

    /** Total number of results found. Useful with paging. */
    private long total;

    /**
     * Ignored parameters.
     *
     * IIIF Search requires parameter 'q'. 'motivation' is recommended and
     * three parameters 'date', 'user', 'box' are optional. Any ignored parameter
     * must be reported here.
     */
    private String[] ignored;

    /** List of 'hits' information containing context for results. */
    private IIIFSearchHit[] hits;

    // Info for paging
    /** Index of the first result in this page within the total */
    private long startIndex;

    // URIs for navigating results paging
    /** URI of next page of results */
    private String next;
    /** URI of previous page of results */
    private String prev;
    /** URI of first results page */
    private String first;
    /** URI of last results page */
    private String last;

    public IIIFSearchResult() {
        super();
        setContext(SEARCH_CONTEXT);
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public String[] getIgnored() {
        return ignored;
    }

    public void setIgnored(String[] ignored) {
        this.ignored = ignored;
    }

    public IIIFSearchHit[] getHits() {
        return hits;
    }

    public void setHits(IIIFSearchHit[] hits) {
        this.hits = hits;
    }

    public long getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(long startIndex) {
        this.startIndex = startIndex;
    }

    public String getNext() {
        return next;
    }

    public void setNext(String next) {
        this.next = next;
    }

    public String getPrev() {
        return prev;
    }

    public void setPrev(String prev) {
        this.prev = prev;
    }

    public String getFirst() {
        return first;
    }

    public void setFirst(String first) {
        this.first = first;
    }

    public String getLast() {
        return last;
    }

    public void setLast(String last) {
        this.last = last;
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

        if (total != that.total) return false;
        if (startIndex != that.startIndex) return false;
        if (!Arrays.deepEquals(ignored, that.ignored)) return false;
        if (!Arrays.deepEquals(hits, that.hits)) return false;
        if (next != null ? !next.equals(that.next) : that.next != null) return false;
        if (prev != null ? !prev.equals(that.prev) : that.prev != null) return false;
        if (first != null ? !first.equals(that.first) : that.first != null) return false;
        return !(last != null ? !last.equals(that.last) : that.last != null);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (total ^ (total >>> 32));
        result = 31 * result + (ignored != null ? Arrays.deepHashCode(ignored) : 0);
        result = 31 * result + (hits != null ? Arrays.deepHashCode(hits) : 0);
        result = 31 * result + (int) (startIndex ^ (startIndex >>> 32));
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
