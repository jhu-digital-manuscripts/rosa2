package rosa.iiif.search.model;

import rosa.iiif.presentation.model.PresentationRequest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Current version: SearchAPI 0.9
 *
 * http://search.iiif.io/api/search/0.9/#request
 *
 * TODO refactor. Instead of individual fields here, use a map String (parameter) -> String[] (query tokens)?
 */
public class IIIFSearchRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * A IIIF Search is initiated within a particular IIIF object. This object is
     * identified here by its request in the IIIF Presentation API, which contains
     *
     * <ul>
     * <li>Object type - Collection, Manifest, Canvas, etc</li>
     * <li>Object identifier - This field does not apply to collections. For a Manifest,
     *     this will contain the manifest ID plus its parent collection. For a Canvas,
     *     this will contain the parent collection + manifest ID</li>
     * <li>Object name - name of object. This field does not apply to Manifests. Names
     *     will appear for collections, canvases, annotations, etc</li>
     * </ul>
     *
     */
    public final PresentationRequest objectId;

    public final String[] queryTerms;

    /**
     * List of motivations. Search among this set of annotation motivations.
     * (See OpenAnnotation:motivation)
     */
    public final String[] motivations;

    /**
     * Search for annotations (created?) on a date, or within a date range.
     * List of dates in ISO8601 format expressed in UTC with z-format
     */
    public final String[] dates;

    /** List of users. Useful when searching among commentary for particular users. */
    public final String[] users;

    /**
     * Search for annotations within boxes (on a canvas): x,y,w,h
     */
    public final Rectangle[] box;

    /** Results page to retrieve, if applicable */
    public final int page;

    public IIIFSearchRequest(PresentationRequest object, String q, String motivations, String dates, String users, String box, int page) {
        this.objectId = object;
        this.queryTerms = toArray(q);
        this.motivations = toArray(motivations);
        this.dates = toArray(dates);
        this.users = toArray(users);
        this.box = rectangles(box);
        this.page = page;
    }

    public IIIFSearchRequest(PresentationRequest object, String q, String motivations, int page) {
        this(object, q, motivations, null, null, null, page);
    }

    /**
     * Construct a new IIIFSearchRequest with dates, users, box(es) ignored.
     *
     * @param object the IIIF ID of the object within which to search
     * @param q query terms
     * @param motivations motivations
     */
    public IIIFSearchRequest(PresentationRequest object, String q, String motivations) {
        this(object, q, motivations, 0);
    }

    /**
     * Construct a new IIIFSearchRequest
     *
     * @param object the IIIF ID of the object within which to search
     * @param q query terms
     * @param page requested results page
     */
    public IIIFSearchRequest(PresentationRequest object, String q, int page) {
        this(object, q, null, page);
    }

    /**
     * Construct a new IIIFSearchRequest with ONLY query terms.
     *
     * @param object the IIIF ID of the object within which to search
     * @param q query terms
     */
    public IIIFSearchRequest(PresentationRequest object, String q) {
        this(object, q, 0);
    }

    /**
     * Split a string on whitespace. If input string is NULL or empty,
     * return array containing no elements (length = 0)
     *
     * @param str input string
     * @return array of tokens split on whitespace
     */
    private String[] toArray(String str) {
        return (str == null || str.isEmpty()) ? new String[0] : str.split("\\s+");
    }

    /**
     * Convert input string into array of rectangles. Each box segment must match: x,y,w,h.
     * Segments are separated by whitespace.
     *
     * @param input input string
     * @return array of rectangles
     * @throws RuntimeException if at least one rectangle is malformed. This can happen if the
     *                          number of rectangle parameters specified is not 4 or if a parameter
     *                          is not a number.
     */
    private Rectangle[] rectangles(String input) {
        String[] boxes = toArray(input);

        List<Rectangle> rectangles = new ArrayList<>();
        for (String box : boxes) {
            String[] parts = box.split(",");

            try {
                rectangles.add(new Rectangle(
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]),
                        Integer.parseInt(parts[3])
                ));
            } catch (IndexOutOfBoundsException | NumberFormatException e) {
                throw new RuntimeException("Malformed rectangle (" + box
                        + ") in parameter [" + input + "]", e);
            }
        }

        return rectangles.toArray(new Rectangle[rectangles.size()]);
    }

//    protected boolean canEqual(Object obj) {
//        return obj instanceof IIIFSearchRequest;
//    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IIIFSearchRequest that = (IIIFSearchRequest) o;

        if (page != that.page) return false;
        if (objectId != null ? !objectId.equals(that.objectId) : that.objectId != null) return false;
        if (!Arrays.deepEquals(queryTerms, that.queryTerms)) return false;
        if (!Arrays.deepEquals(motivations, that.motivations)) return false;
        if (!Arrays.deepEquals(dates, that.dates)) return false;
        if (!Arrays.deepEquals(users, that.users)) return false;
        return Arrays.deepEquals(box, that.box);

    }

    @Override
    public int hashCode() {
        int result = objectId != null ? objectId.hashCode() : 0;
        result = 31 * result + (queryTerms != null ? Arrays.deepHashCode(queryTerms) : 0);
        result = 31 * result + (motivations != null ? Arrays.deepHashCode(motivations) : 0);
        result = 31 * result + (dates != null ? Arrays.deepHashCode(dates) : 0);
        result = 31 * result + (users != null ? Arrays.deepHashCode(users) : 0);
        result = 31 * result + (box != null ? Arrays.deepHashCode(box) : 0);
        result = 31 * result + page;
        return result;
    }

    @Override
    public String toString() {
        return "IIIFSearchRequest{" +
                "objectId='" + objectId + '\'' +
                ", queryTerms=" + Arrays.toString(queryTerms) +
                ", motivations=" + Arrays.toString(motivations) +
                ", dates=" + Arrays.toString(dates) +
                ", users=" + Arrays.toString(users) +
                ", box=" + Arrays.toString(box) +
                ", page=" + page +
                '}';
    }
}
