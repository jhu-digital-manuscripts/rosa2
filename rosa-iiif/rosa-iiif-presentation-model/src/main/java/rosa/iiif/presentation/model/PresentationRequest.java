package rosa.iiif.presentation.model;

/**
 * Object representing a request to the IIIF Presentation endpoint. This object records
 * the requested object name/id.
 *
 * IIIF Collection has a name, but no ID.
 * Other IIIF objects have IDs.
 */
public class PresentationRequest {
    private PresentationRequestType type;
    private String id;
    private String name;

    /**
     * Create an empty PresentationRequest
     */
    public PresentationRequest() {}

    /**
     * Create a PresentationRequests with data.
     *
     * @param id requested ID, including collection and book
     * @param name requested name
     * @param type requested type
     */
    public PresentationRequest(String id, String name, PresentationRequestType type) {
        this.type = type;
        this.id = id;
        this.name = name;
    }

    public PresentationRequestType getType() {
        return type;
    }

    public void setType(PresentationRequestType type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof PresentationRequest))
            return false;
        PresentationRequest other = (PresentationRequest) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (type != other.type)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "PresentationRequest [type=" + type + ", id=" + id + ", name=" + name + "]";
    }
}
