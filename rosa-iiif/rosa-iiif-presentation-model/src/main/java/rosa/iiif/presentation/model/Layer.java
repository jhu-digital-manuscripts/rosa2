package rosa.iiif.presentation.model;

import java.util.ArrayList;
import java.util.List;


public class Layer extends PresentationBase {
    private static final long serialVersionUID = 1L;
    
    private List<String> otherContent;

    public Layer() {
        this.otherContent = new ArrayList<>();
    }
    
    public List<String> getOtherContent() {
        return otherContent;
    }

    public void setOtherContent(List<String> otherContent) {
        this.otherContent = otherContent;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((otherContent == null) ? 0 : otherContent.hashCode());
        return result;
    }

    @Override
    protected boolean canEqual(Object obj) {
        return obj instanceof Layer;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (!(obj instanceof Layer))
            return false;
        Layer other = (Layer) obj;
        
        if (!other.canEqual(this)) {
            return false;
        }
        
        if (otherContent == null) {
            if (other.otherContent != null)
                return false;
        } else if (!otherContent.equals(other.otherContent))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Layer [otherContent=" + otherContent + "]";
    }
}
