package rosa.archive.model.aor;

import java.io.Serializable;

/**
 *
 */
public abstract class Annotation implements Serializable {

    private String referringText;

    public String getReferringText() {
        return referringText;
    }

    public void setReferringText(String referringText) {
        this.referringText = referringText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Annotation that = (Annotation) o;

        if (referringText != null ? !referringText.equals(that.referringText) : that.referringText != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return referringText != null ? referringText.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Annotation{" +
                "referringText='" + referringText + '\'' +
                '}';
    }
}
