package rosa.iiif.presentation.model.annotation;

import rosa.iiif.presentation.model.selector.Selector;

import java.io.Serializable;

public class AnnotationTarget implements Serializable {
    private static final long serialVersionUID = 1L;

    protected String uri;
    protected Selector selector;

    public AnnotationTarget() {}

    public AnnotationTarget(String uri) {
        this(uri, null);
    }

    public AnnotationTarget(String uri, Selector selector) {
        this.uri = uri;
        this.selector = selector;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((selector == null) ? 0 : selector.hashCode());
        result = prime * result + ((uri == null) ? 0 : uri.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof AnnotationTarget))
            return false;
        AnnotationTarget other = (AnnotationTarget) obj;
        if (selector == null) {
            if (other.selector != null)
                return false;
        } else if (!selector.equals(other.selector))
            return false;
        if (uri == null) {
            if (other.uri != null)
                return false;
        } else if (!uri.equals(other.uri))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "AnnotationTarget [uri=" + uri + ", selector=" + selector + "]";
    }
}
