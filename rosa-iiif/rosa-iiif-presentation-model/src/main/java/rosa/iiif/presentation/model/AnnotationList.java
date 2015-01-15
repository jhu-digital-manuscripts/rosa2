package rosa.iiif.presentation.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import rosa.iiif.presentation.model.annotation.Annotation;

public class AnnotationList extends PresentationBase implements Iterable<Annotation> {
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
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((annotations == null) ? 0 : annotations.hashCode());
        return result;
    }
    
    @Override
    protected boolean canEqual(Object obj) {
        return obj instanceof AnnotationList;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (!(obj instanceof AnnotationList))
            return false;
        AnnotationList other = (AnnotationList) obj;
        
        if (!other.canEqual(this)) {
            return false;
        }
        
        if (annotations == null) {
            if (other.annotations != null)
                return false;
        } else if (!annotations.equals(other.annotations))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "AnnotationList [annotations=" + annotations + "]";
    }
}
