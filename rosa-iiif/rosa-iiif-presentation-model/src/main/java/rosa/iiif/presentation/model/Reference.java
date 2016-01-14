package rosa.iiif.presentation.model;

import java.io.Serializable;

/**
 * A reference to another object. It contains the URI, type, and label of an
 * object.
 */
public class Reference implements Serializable {
    private static final long serialVersionUID = 1L;

    private String ref;
    private TextValue label;
    private String type;

    public Reference() {}

    public Reference(String ref, TextValue label, String type) {
        this.ref = ref;
        this.label = label;
        this.type = type;
    }

    public String getReference() {
        return ref;
    }

    public void setReference(String ref) {
        this.ref = ref;
    }

    public TextValue getLabel() {
        return label;
    }

    public void setLabel(TextValue label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((label == null) ? 0 : label.hashCode());
        result = prime * result + ((ref == null) ? 0 : ref.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Reference))
            return false;
        Reference other = (Reference) obj;
        if (label == null) {
            if (other.label != null)
                return false;
        } else if (!label.equals(other.label))
            return false;
        if (ref == null) {
            if (other.ref != null)
                return false;
        } else if (!ref.equals(other.ref))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Reference [ref=" + ref + ", label=" + label + ", type=" + type + "]";
    }
}
