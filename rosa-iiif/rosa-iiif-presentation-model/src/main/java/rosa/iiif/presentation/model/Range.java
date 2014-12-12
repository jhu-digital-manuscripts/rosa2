package rosa.iiif.presentation.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * It may be important to describe additional structure within an object, such as newspaper articles
 * that span pages, the range of non-content-bearing pages at the beginning of a work, or chapters
 * within a book. These are described using ranges in a similar manner to sequences. Ranges must have
 * URIs and they should be http(s) URIs. The intent of adding a range to the manifest is to allow the
 * client to display a structured hierarchy to enable the user to navigate within the object without
 * merely stepping through the current sequence. The rationale for separating ranges from sequences
 * is that there is likely to be overlap between different ranges, such as the physical structure of a
 * book compared to the textual structure of the work. An example would be a newspaper with articles
 * that are continued in different sections, or simply a section that starts half way through a page.
 *
 * A range will typically include one or more canvases or, unlike sequences, parts of canvases. The part
 * must be rectangular, and is given using the xywh= fragment approach. This allows for selecting, for
 * example, the areas within two newspaper pages where an article is located. As the information about
 * the canvas is already in the sequence, it must not be repeated. In order to present a table of the
 * different ranges to allow a user to select one, every range must have a label and the top most range
 * in the table should have a viewingHint with the value “top”. A range that is the top of a hierarchy
 * does not need to list all of the canvases in the sequence, and should only give the list of ranges
 * below it. Ranges may also have any of the other properties defined in this specification, including
 * the startCanvas relationship to the first canvas within the range to start with, if it is not the
 * first listed in canvases.
 *
 * Ranges may include other ranges. This is done in a ranges property within the range. The values
 * within the ranges list must be strings giving the URIs of ranges in the list in the manifest.
 *
 * Ranges are linked or embedded within the manifest in a structures field. It is a flat list of objects,
 * even if there is only one range.
 */
public class Range extends PresentationBase implements Serializable {
    private static final long serialVersionUID = 1L;

    private ViewingDirection viewingDirection;
    private int startingCanvas;

    private List<String> canvases;
    private List<Range> ranges;

    public Range() {
        canvases = new ArrayList<>();
        ranges = new ArrayList<>();
    }

    public ViewingDirection getViewingDirection() {
        return viewingDirection;
    }

    public void setViewingDirection(ViewingDirection viewingDirection) {
        this.viewingDirection = viewingDirection;
    }

    public int getStartingCanvas() {
        return startingCanvas;
    }

    public void setStartingCanvas(int startingCanvas) {
        this.startingCanvas = startingCanvas;
    }

    public List<String> getCanvases() {
        return canvases;
    }

    public void setCanvases(List<String> canvases) {
        this.canvases = canvases;
    }

    public List<Range> getRanges() {
        return ranges;
    }

    public void setRanges(List<Range> ranges) {
        this.ranges = ranges;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Range range = (Range) o;

        if (startingCanvas != range.startingCanvas) return false;
        if (canvases != null ? !canvases.equals(range.canvases) : range.canvases != null) return false;
        if (ranges != null ? !ranges.equals(range.ranges) : range.ranges != null) return false;
        if (viewingDirection != range.viewingDirection) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (viewingDirection != null ? viewingDirection.hashCode() : 0);
        result = 31 * result + startingCanvas;
        result = 31 * result + (canvases != null ? canvases.hashCode() : 0);
        result = 31 * result + (ranges != null ? ranges.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Range{" +
                super.toString() +
                "viewingDirection=" + viewingDirection +
                ", startingCanvas=" + startingCanvas +
                ", canvases=" + canvases +
                ", ranges=" + ranges +
                '}';
    }
}
