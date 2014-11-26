package rosa.iiif.presentation.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A collection of items. Can contain multiple sub-collections within the current
 * collection and/or multiple manifests.
 *
 * Collections MUST have an id and type, and SHOULD have metadata, description,
 * and thumbnail. Other fields are optional.
 */
public class Collection extends PresentationBase implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<Collection> collections;
    private List<Manifest> manifests;

    public Collection() {
        collections = new ArrayList<>();
        manifests = new ArrayList<>();
    }

    public List<Collection> getCollections() {
        return collections;
    }

    public void setCollections(List<Collection> collections) {
        this.collections = collections;
    }

    public List<Manifest> getManifests() {
        return manifests;
    }

    public void setManifests(List<Manifest> manifests) {
        this.manifests = manifests;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Collection that = (Collection) o;

        if (collections != null ? !collections.equals(that.collections) : that.collections != null) return false;
        if (manifests != null ? !manifests.equals(that.manifests) : that.manifests != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (collections != null ? collections.hashCode() : 0);
        result = 31 * result + (manifests != null ? manifests.hashCode() : 0);
        return result;
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
