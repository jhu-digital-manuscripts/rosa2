package rosa.iiif.presentation.model;

import java.util.ArrayList;
import java.util.List;

public class Manifest extends PresentationBase {
    private static final long serialVersionUID = 1L;

    private Sequence defaultSequence;    
    private List<Reference> otherSequences;
    private List<Range> ranges;
    private List<Reference> otherRanges;

    public Manifest() {
        this(null, null);
    }

    public Manifest(ViewingDirection viewingDirection, Sequence sequence) {
        super();
        this.viewingDirection = viewingDirection;
        this.defaultSequence = sequence;
        this.otherSequences = new ArrayList<>();
        this.ranges = new ArrayList<>();
        this.otherRanges = new ArrayList<>();
        
        setType(IIIFNames.SC_MANIFEST);
    }

    public ViewingDirection getViewingDirection() {
        return viewingDirection;
    }

    public void setViewingDirection(ViewingDirection viewingDirection) {
        this.viewingDirection = viewingDirection;
    
    }

    public List<Range> getRanges() {
        return ranges;
    }

    public void setRanges(List<Range> ranges) {
        this.ranges = ranges;
    }

    public List<Reference> getOtherRanges() {
        return otherRanges;
    }

    public void setOtherRanges(List<Reference> otherRanges) {
        this.otherRanges = otherRanges;
    }

    public List<Reference> getOtherSequences() {
        return otherSequences;
    }

    public void setOtherSequences(List<Reference> sequences) {
        this.otherSequences = sequences;
    }

    public Sequence getDefaultSequence() {
        return defaultSequence;
    }

    public void setDefaultSequence(Sequence defaultSequence) {
        this.defaultSequence = defaultSequence;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((defaultSequence == null) ? 0 : defaultSequence.hashCode());
        result = prime * result + ((otherRanges == null) ? 0 : otherRanges.hashCode());
        result = prime * result + ((otherSequences == null) ? 0 : otherSequences.hashCode());
        result = prime * result + ((ranges == null) ? 0 : ranges.hashCode());
        return result;
    }
    
    @Override
    protected boolean canEqual(Object obj) {
        return obj instanceof Manifest;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (!(obj instanceof Manifest))
            return false;
        Manifest other = (Manifest) obj;
        
        if (!other.canEqual(this)) {
            return false;
        }
        if (defaultSequence == null) {
            if (other.defaultSequence != null)
                return false;
        } else if (!defaultSequence.equals(other.defaultSequence))
            return false;
        if (otherRanges == null) {
            if (other.otherRanges != null)
                return false;
        } else if (!otherRanges.equals(other.otherRanges))
            return false;
        if (otherSequences == null) {
            if (other.otherSequences != null)
                return false;
        } else if (!otherSequences.equals(other.otherSequences))
            return false;
        if (ranges == null) {
            if (other.ranges != null)
                return false;
        } else if (!ranges.equals(other.ranges))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Manifest [defaultSequence=" + defaultSequence + ", otherSequences=" + otherSequences + ", ranges="
                + ranges + ", otherRanges=" + otherRanges + "]";
    }
}
