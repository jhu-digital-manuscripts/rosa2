package rosa.archive.model.aor;

import java.io.Serializable;

/**
 *
 */
public abstract class Annotation implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String text;
    private Location location;
    private String language;
    private String imageId;

    protected Annotation() {}

    protected Annotation(String id, String refText, String language, String imageId, Location location) {
        this.id = id;
        this.text = refText;
        this.location = location;
        this.language = language;
        this.imageId = imageId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public abstract String toPrettyString();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Annotation that = (Annotation) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (text != null ? !text.equals(that.text) : that.text != null) return false;
        if (location != that.location) return false;
        if (language != null ? !language.equals(that.language) : that.language != null) return false;
        return imageId != null ? imageId.equals(that.imageId) : that.imageId == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (text != null ? text.hashCode() : 0);
        result = 31 * result + (location != null ? location.hashCode() : 0);
        result = 31 * result + (language != null ? language.hashCode() : 0);
        result = 31 * result + (imageId != null ? imageId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Annotation{" +
                "id='" + id + '\'' +
                ", text='" + text + '\'' +
                ", location=" + location +
                ", language='" + language + '\'' +
                ", imageId='" + imageId + '\'' +
                '}';
    }
}
