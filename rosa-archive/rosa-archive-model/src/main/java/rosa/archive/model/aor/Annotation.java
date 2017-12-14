package rosa.archive.model.aor;

import java.io.Serializable;
import java.util.Objects;

/**
 *
 */
public abstract class Annotation implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String text;
    private Location location;
    private String language;

    private String internalRef;

    private boolean generatedId;

    protected Annotation() {}

    protected Annotation(String id, String refText, String language, Location location) {
        this.id = id;
        this.text = refText;
        this.location = location;
        this.language = language;
        this.generatedId = false;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setId(String id, boolean generatedId) {
        this.id = id;
        this.generatedId = generatedId;
    }

    public String getReferencedText() {
        return text;
    }

    public void setReferencedText(String refText) {
        this.text = refText;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public abstract String toPrettyString();

    public String getInternalRef() {
        return internalRef;
    }

    public void setInternalRef(String internalRef) {
        this.internalRef = internalRef;
    }

    public boolean isGeneratedId() {
        return generatedId;
    }

    public void setGeneratedId(boolean generatedId) {
        this.generatedId = generatedId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Annotation that = (Annotation) o;
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
