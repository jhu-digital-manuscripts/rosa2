package rosa.iiif.presentation.model;

import rosa.iiif.presentation.model.annotation.Annotation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AnnotationList extends PresentationBase implements Iterable<Annotation>, Serializable {
    private static final long serialVersionUID = 1L;

    private List<Annotation> annotations;

    public AnnotationList() {
        super();
        annotations = new ArrayList<>();

        setType(SC_ANNOTATION_LIST);
    }

    public List<Annotation> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<Annotation> annotations) {
        this.annotations = annotations;
    }

    public int size() {
        return annotations.size();
    }

    @Override
    public Iterator<Annotation> iterator() {
        return annotations.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        AnnotationList that = (AnnotationList) o;

        if (annotations != null ? !annotations.equals(that.annotations) : that.annotations != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (annotations != null ? annotations.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "AnnotationList{" +
                super.toString() +
                "annotations=" + annotations +
                '}';
    }
}
