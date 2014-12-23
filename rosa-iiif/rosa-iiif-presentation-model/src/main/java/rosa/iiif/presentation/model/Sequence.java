package rosa.iiif.presentation.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The sequence conveys the ordering of the views of the object. The default sequence
 * (and typically the only sequence) must be embedded within the manifest, and may also
 * be available from its own URI. The default sequence may have a URI to identify it.
 * Any additional sequences must be referred to from the manifest, not embedded within
 * it, and thus these additional sequences must have an HTTP URI.
 *
 * The new {name} parameter in the URI structure must distinguish it from any other
 * sequences that may be available for the physical object. Typical default names for
 * sequences are “normal” or “basic”. Names should begin with an alphabetical character.
 *
 * Sequences may have their own descriptive, rights and linking metadata using the same
 * fields as for manifests. The label property may be given for sequences and must be
 * given if there is more than one referenced from a manifest. After the metadata, the set
 * of pages in the object, represented by canvas resources, are listed in order in the
 * canvases property. There must be at least one canvas given.
 *
 * Sequences may have a startCanvas with a single value containing the URI of a canvas
 * resource that is contained within the sequence. This is the canvas that a viewer should
 * initialize its display with for the user. If it is not present, then the viewer should
 * use the first canvas in the sequence.
 *
 * In the manifest example above, the sequence is referenced by its URI and contains only
 * the basic information of label, @type and @id. The default sequence should be written out
 * in full within the manifest file, as below but must not have the @context property.
 */
public class Sequence extends PresentationBase implements Iterable<Canvas>, Serializable {
    private static final long serialVersionUID = 1L;

    private ViewingDirection viewingDirection;
    private int startCanvas;

    private List<Canvas> canvases;

    public Sequence() {
        super();
        canvases = new ArrayList<>();
        startCanvas = -1;

        setType(IIIFNames.SC_SEQUENCE);
    }

    public ViewingDirection getViewingDirection() {
        return viewingDirection;
    }

    public void setViewingDirection(ViewingDirection viewingDirection) {
        this.viewingDirection = viewingDirection;
    }

    public int getStartCanvas() {
        return startCanvas;
    }

    public void setStartCanvas(int startCanvas) {
        this.startCanvas = startCanvas;
    }

    public List<Canvas> getCanvases() {
        return canvases;
    }

    public void setCanvases(List<Canvas> canvases) {
        this.canvases = canvases;
    }

    @Override
    public Iterator<Canvas> iterator() {
        return canvases.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Sequence canvases1 = (Sequence) o;

        if (startCanvas != canvases1.startCanvas) return false;
        if (canvases != null ? !canvases.equals(canvases1.canvases) : canvases1.canvases != null) return false;
        if (viewingDirection != canvases1.viewingDirection) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (viewingDirection != null ? viewingDirection.hashCode() : 0);
        result = 31 * result + startCanvas;
        result = 31 * result + (canvases != null ? canvases.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Sequence{" +
                super.toString() +
                "viewingDirection=" + viewingDirection +
                ", startCanvas=" + startCanvas +
                ", canvases=" + canvases +
                '}';
    }
}
