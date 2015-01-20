package rosa.iiif.presentation.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A collection of items. Can contain multiple sub-collections within the current
 * collection and/or multiple manifests.
 *
 * Collections MUST have an id and type, and SHOULD have metadata, description,
 * and thumbnail. Other fields are optional.
 */
public class Collection extends PresentationBase {
    private static final long serialVersionUID = 1L;

    private List<Reference> collections;
    private List<Reference> manifests;

    public Collection() {
        collections = new ArrayList<>();
        manifests = new ArrayList<>();
        setType(IIIFNames.SC_COLLECTION);
    }

    public List<Reference> getCollections() {
        return collections;
    }

    public void setCollections(List<Reference> collections) {
        this.collections = collections;
    }

    public List<Reference> getManifests() {
        return manifests;
    }

    public void setManifests(List<Reference> manifests) {
        this.manifests = manifests;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((collections == null) ? 0 : collections.hashCode());
        result = prime * result + ((manifests == null) ? 0 : manifests.hashCode());
        return result;
    }

    @Override
    protected boolean canEqual(Object obj) {
        return obj instanceof Collection;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (!(obj instanceof Collection))
            return false;
        Collection other = (Collection) obj;
        
        if (!other.canEqual(this)) {
            return false;
        }
        
        if (collections == null) {
            if (other.collections != null)
                return false;
        } else if (!collections.equals(other.collections))
            return false;
        if (manifests == null) {
            if (other.manifests != null)
                return false;
        } else if (!manifests.equals(other.manifests))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Collection{" +
                super.toString() +
                "collections=" + collections +
                ", manifests=" + manifests +
                '}';
    }
}
