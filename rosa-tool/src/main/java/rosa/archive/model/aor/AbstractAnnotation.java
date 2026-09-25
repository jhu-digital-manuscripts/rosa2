package rosa.archive.model.aor;

import java.util.Objects;

/**
 * Base implementation of common annotation fields.
 * Provides shared state management for all annotation types.
 */
abstract sealed class AbstractAnnotation implements Annotation permits Marginalia, Drawing, Graph, Table, Calculation, Errata, Numeral, Mark, Symbol, Underline, PhysicalLink {

    private String id;
    private String text;
    private Location location;
    private String language;
    private String internalRef;
    private boolean generatedId;

    /**
     * Creates an annotation with no initial values.
     */
    protected AbstractAnnotation() {}

    /**
     * Creates an annotation with the specified base fields.
     *
     * @param id       the annotation identifier
     * @param refText  the referenced text
     * @param language the language code
     * @param location the physical location on the page
     */
    protected AbstractAnnotation(String id, String refText, String language, Location location) {
        this.id = id;
        this.text = refText;
        this.location = location;
        this.language = language;
        this.generatedId = false;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Sets the annotation ID, optionally marking it as generated.
     *
     * @param id          the annotation ID
     * @param generatedId whether the ID was generated
     */
    public void setId(String id, boolean generatedId) {
        this.id = id;
        this.generatedId = generatedId;
    }

    @Override
    public String getReferencedText() {
        return text;
    }

    @Override
    public void setReferencedText(String refText) {
        this.text = refText;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public String getLanguage() {
        return language;
    }

    @Override
    public void setLanguage(String language) {
        this.language = language;
    }

    @Override
    public String getInternalRef() {
        return internalRef;
    }

    @Override
    public void setInternalRef(String internalRef) {
        this.internalRef = internalRef;
    }

    @Override
    public boolean isGeneratedId() {
        return generatedId;
    }

    @Override
    public void setGeneratedId(boolean generatedId) {
        this.generatedId = generatedId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractAnnotation that = (AbstractAnnotation) o;
        return generatedId == that.generatedId &&
                Objects.equals(id, that.id) &&
                Objects.equals(text, that.text) &&
                location == that.location &&
                Objects.equals(language, that.language) &&
                Objects.equals(internalRef, that.internalRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, text, location, language, internalRef, generatedId);
    }

    @Override
    public String toString() {
        return "Annotation{" +
                "id='" + id + '\'' +
                ", text='" + text + '\'' +
                ", location=" + location +
                ", language='" + language + '\'' +
                ", internalRef='" + internalRef + '\'' +
                ", generatedId=" + generatedId +
                '}';
    }
}
