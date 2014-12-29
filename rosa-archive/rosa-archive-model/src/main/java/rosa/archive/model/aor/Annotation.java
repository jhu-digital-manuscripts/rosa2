package rosa.archive.model.aor;

import java.io.Serializable;

/**
 *
 */
public abstract class Annotation implements Serializable {
    private static final long serialVersionUID = 1L;

    private String referringText;
    private Location location;

    protected Annotation() {}

    protected Annotation(String referringText, Location location) {
        this.referringText = referringText;
        this.location = location;
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

    public abstract String toPrettyString();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Annotation that = (Annotation) o;

        if (location != that.location) return false;
        if (referringText != null ? !referringText.equals(that.referringText) : that.referringText != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = referringText != null ? referringText.hashCode() : 0;
        result = 31 * result + (location != null ? location.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Annotation{" +
                "referringText='" + referringText + '\'' +
                ", location=" + location +
                '}';
    }
}
