package rosa.iiif.presentation.model;

import java.io.Serializable;
import java.util.Objects;

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
 *
 * A client may want to build a list of references and sort them before
 * displaying the information to a user. The sorting tag can be used to
 * add information to the reference to be used for sorting that will
 * (most likely) not be displayed to a user.
 */
public class Reference extends PresentationBase implements Serializable {
    private static final long serialVersionUID = 1L;

    private String sortingTag;

    public Reference() {}

    public Reference(String ref, TextValue label, String type) {
        this(ref, label, type, ref);
    }

    public Reference(String ref, TextValue label, String type, String sortingTag) {
        super();
        this.id = ref;
        this.label = label;
        this.type = type;
        this.sortingTag = sortingTag;
    }

    public String getReference() {
        return getId();
    }

    public void setReference(String ref) {
        setId(ref);
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

    public String getSortingTag() {
        return sortingTag;
    }

    public void setSortingTag(String sortingTag) {
        this.sortingTag = sortingTag;
    }

    public void addSortingTag(String tag) {
        if (tag == null) {
            return;
        }
        if (this.sortingTag == null) {
            this.sortingTag = tag;
        } else {
            this.sortingTag += tag;
        }
    }

    @Override
    protected boolean canEqual(Object o) {
        return o instanceof Reference;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
//        if (!(o instanceof Reference)) return false;
        Reference reference = (Reference) o;

        return reference.canEqual(this) && Objects.equals(sortingTag, reference.sortingTag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), sortingTag);
    }

    @Override
    public String toString() {
        return "Reference{" +
                "sortingTag='" + sortingTag + "' " +
                super.toString() +
                '}';
    }
}
