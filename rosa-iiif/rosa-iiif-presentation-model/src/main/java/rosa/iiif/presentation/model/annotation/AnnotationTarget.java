package rosa.iiif.presentation.model.annotation;

import rosa.iiif.presentation.model.Reference;
import rosa.iiif.presentation.model.selector.Selector;

import java.io.Serializable;

public class AnnotationTarget implements Serializable {
    private static final long serialVersionUID = 1L;

    protected String uri;
    protected Selector selector;
    protected Reference parentRef;

    public AnnotationTarget() {}

    public AnnotationTarget(String uri) {
        this(uri, null);
    }

    public AnnotationTarget(String uri, Selector selector) {
        this.uri = uri;
        this.selector = selector;
    }

    public Reference getParentRef() {
        return parentRef;
    }

    public void setParentRef(Reference parentRef) {
        this.parentRef = parentRef;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Selector getSelector() {
        return selector;
    }

    public void setSelector(Selector selector) {
        this.selector = selector;
    }

    public boolean isSpecificResource() {
        return selector != null;
    }

    public boolean canEqual(Object o) {
        return o instanceof AnnotationTarget;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof AnnotationTarget)) return false;

        AnnotationTarget that = (AnnotationTarget) o;
        if (!that.canEqual(this)) return false;

        if (uri != null ? !uri.equals(that.uri) : that.uri != null) return false;
        if (selector != null ? !selector.equals(that.selector) : that.selector != null) return false;
        return !(parentRef != null ? !parentRef.equals(that.parentRef) : that.parentRef != null);

    }

    @Override
    public int hashCode() {
        int result = uri != null ? uri.hashCode() : 0;
        result = 31 * result + (selector != null ? selector.hashCode() : 0);
        result = 31 * result + (parentRef != null ? parentRef.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "AnnotationTarget{" +
                "uri='" + uri + '\'' +
                ", selector=" + selector +
                ", parentRef=" + parentRef +
                '}';
    }
}
