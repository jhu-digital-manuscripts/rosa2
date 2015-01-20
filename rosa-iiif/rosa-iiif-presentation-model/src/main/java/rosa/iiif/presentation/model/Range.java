package rosa.iiif.presentation.model;

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
public class Range extends PresentationBase {
    private static final long serialVersionUID = 1L;

    private int startingCanvas;

    private List<String> canvases;
    private List<String> ranges;

    public Range() {
        canvases = new ArrayList<>();
        ranges = new ArrayList<>();
        setType(IIIFNames.SC_RANGE);
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

    public List<String> getRanges() {
        return ranges;
    }

    public void setRanges(List<String> ranges) {
        this.ranges = ranges;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((canvases == null) ? 0 : canvases.hashCode());
        result = prime * result + ((ranges == null) ? 0 : ranges.hashCode());
        result = prime * result + startingCanvas;
        return result;
    }

    @Override
    protected boolean canEqual(Object obj) {
        return obj instanceof Range;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (!(obj instanceof Range))
            return false;
        Range other = (Range) obj;
        
        if (!other.canEqual(this)) {
            return false;
        }
        
        if (canvases == null) {
            if (other.canvases != null)
                return false;
        } else if (!canvases.equals(other.canvases))
            return false;
        if (ranges == null) {
            if (other.ranges != null)
                return false;
        } else if (!ranges.equals(other.ranges))
            return false;
        if (startingCanvas != other.startingCanvas)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Range [startingCanvas=" + startingCanvas + ", canvases=" + canvases + ", ranges=" + ranges + "]";
    }
}
