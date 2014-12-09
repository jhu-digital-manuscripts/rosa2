package rosa.iiif.presentation.model;

import java.io.Serializable;
import java.util.List;

public class Manifest extends PresentationBase implements Serializable {
    private static final long serialVersionUID = 1L;

    private ViewingDirection viewingDirection;
    private List<Sequence> sequences;

    public Manifest() {}

    public Manifest(ViewingDirection viewingDirection, List<Sequence> sequences) {
        this.viewingDirection = viewingDirection;
        this.sequences = sequences;
    }

    public ViewingDirection getViewingDirection() {
        return viewingDirection;
    }

    public void setViewingDirection(ViewingDirection viewingDirection) {
        this.viewingDirection = viewingDirection;
    }

    public List<Sequence> getSequences() {
        return sequences;
    }

    public void setSequences(List<Sequence> sequences) {
        this.sequences = sequences;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Manifest manifest = (Manifest) o;

        if (sequences != null ? !sequences.equals(manifest.sequences) : manifest.sequences != null) return false;
        if (viewingDirection != manifest.viewingDirection) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (viewingDirection != null ? viewingDirection.hashCode() : 0);
        result = 31 * result + (sequences != null ? sequences.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Manifest{" +
                super.toString() +
                "viewingDirection=" + viewingDirection +
                ", sequences=" + sequences +
                '}';
    }
}
