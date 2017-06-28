package rosa.iiif.presentation.model;

import java.io.Serializable;

/**
 * A reference to another object. It contains the URI, type, and label of an
 * object.
 *
 * A reference in IIIF MAY contain extra metadata relating to the object.
 * For example, a collection may contain a reference to a manifest. That
 * manifest reference must have the manifest ID, label, and object type, but
 * it also may include other data such as thumbnails and an arbitrary
 * metadata map. The reference must not include references to other IIIF
 * objects.
 */
public class Reference extends PresentationBase implements Serializable {
    private static final long serialVersionUID = 1L;

    public Reference() {}

    public Reference(String ref, TextValue label, String type) {
        super();
        this.id = ref;
        this.label = label;
        this.type = type;
    }

    public String getReference() {
        return id;
    }

    public void setReference(String ref) {
        this.id = ref;
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

    // No new information is actually added here on top of PresentationBase.
    // #equals and #hashCode are therefore the same

    @Override
    public String toString() {
        return "Reference{" + super.toString() + '}';
    }
}
