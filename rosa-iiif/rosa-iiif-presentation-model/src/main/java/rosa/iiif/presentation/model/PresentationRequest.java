package rosa.iiif.presentation.model;

import java.util.Arrays;

/**
 * Instance represents an IIIF Presentation API request for an object.
 * The object is identified by a list of strings.
 */
public class PresentationRequest {
    private PresentationRequestType type;
    private String[] identifier;

    /**
     * Create an empty PresentationRequest
     */
    public PresentationRequest() {}

    /**
     * Create a PresentationRequests with data.
     *
     * @param identifier ID of requested object
     * @param type requested type
     */
    public PresentationRequest(PresentationRequestType type, String... identifier) {
        this.type = type;
        this.identifier = identifier;
    }
    
    public PresentationRequestType getType() {
        return type;
    }

    public void setType(PresentationRequestType type) {
        this.type = type;
    }

    public String[] getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String[] identifier) {
        this.identifier = identifier;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(identifier);
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PresentationRequest other = (PresentationRequest) obj;
        if (!Arrays.equals(identifier, other.identifier))
            return false;
        if (type != other.type)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "PresentationRequest [type=" + type + ", identifier=" + Arrays.toString(identifier) + "]";
    }
}
