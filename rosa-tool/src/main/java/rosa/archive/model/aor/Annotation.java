package rosa.archive.model.aor;

/**
 * Sealed interface representing an annotation on a page of a book.
 * All concrete annotation types extend {@link AbstractAnnotation}.
 */
public sealed interface Annotation permits AbstractAnnotation {

    /**
     * Returns the annotation identifier.
     *
     * @return the annotation ID, or null if not assigned
     */
    String getId();

    /**
     * Sets the annotation identifier.
     *
     * @param id the annotation ID
     */
    void setId(String id);

    /**
     * Returns the text referenced by this annotation (e.g. underlined text, marked text).
     *
     * @return the referenced text, or null
     */
    String getReferencedText();

    /**
     * Sets the referenced text.
     *
     * @param text the referenced text
     */
    void setReferencedText(String text);

    /**
     * Returns the physical location of this annotation on the page.
     *
     * @return the location, or null
     */
    Location getLocation();

    /**
     * Sets the physical location.
     *
     * @param location the location
     */
    void setLocation(Location location);

    /**
     * Returns the language code of this annotation.
     *
     * @return the language code, or null
     */
    String getLanguage();

    /**
     * Sets the language code.
     *
     * @param language the language code
     */
    void setLanguage(String language);

    /**
     * Returns the internal reference identifier.
     *
     * @return the internal reference, or null
     */
    String getInternalRef();

    /**
     * Sets the internal reference identifier.
     *
     * @param internalRef the internal reference
     */
    void setInternalRef(String internalRef);

    /**
     * Returns whether this annotation's ID was generated rather than specified in the source.
     *
     * @return true if the ID was generated
     */
    boolean isGeneratedId();

    /**
     * Sets whether the ID was generated.
     *
     * @param generatedId true if the ID was generated
     */
    void setGeneratedId(boolean generatedId);

    /**
     * Returns a human-readable summary of this annotation.
     *
     * @return a pretty-printed string representation
     */
    String toPrettyString();
}
