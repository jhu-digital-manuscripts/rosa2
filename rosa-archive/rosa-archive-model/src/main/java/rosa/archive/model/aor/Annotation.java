package rosa.archive.model.aor;

import java.io.Serializable;

/**
 *
 */
public abstract class Annotation implements Serializable {
    private static final long serialVersionUID = 1L;

    private String referringText;
    private Location location;
    private String language;

    protected Annotation() {}

    protected Annotation(String referringText, String language, Location location) {
        this.referringText = referringText;
        this.location = location;
        this.language = language;
    }

    public String getReferringText() {
        return referringText;
    }

    public void setReferringText(String referringText) {
        this.referringText = referringText;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Annotation that = (Annotation) o;

        if (language != null ? !language.equals(that.language) : that.language != null) return false;
        if (location != that.location) return false;
        if (referringText != null ? !referringText.equals(that.referringText) : that.referringText != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = referringText != null ? referringText.hashCode() : 0;
        result = 31 * result + (location != null ? location.hashCode() : 0);
        result = 31 * result + (language != null ? language.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Annotation{" +
                "referringText='" + referringText + '\'' +
                ", location=" + location +
                ", language='" + language + '\'' +
                '}';
    }
}
