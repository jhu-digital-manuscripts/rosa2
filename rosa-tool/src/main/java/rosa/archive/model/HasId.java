package rosa.archive.model;

/**
 * Interface for model objects that have a string identifier.
 */
public interface HasId {

    /**
     * Returns the identifier for this object.
     *
     * @return the identifier, or {@code null} if not set
     */
    String getId();

    /**
     * Sets the identifier for this object.
     *
     * @param id the identifier to set
     */
    void setId(String id);
}
