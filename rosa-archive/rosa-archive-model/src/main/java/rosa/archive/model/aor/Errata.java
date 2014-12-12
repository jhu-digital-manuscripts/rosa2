package rosa.archive.model.aor;

import java.io.Serializable;

/**
 *
 */
public class Errata extends Annotation implements Serializable {

    private String amendedText;

    public Errata() {}

    @Override
    public String toPrettyString() {
        return null;
    }

    public Errata(String copyText, String amendedText) {
        super(copyText, Location.INTEXT);
        this.amendedText = amendedText;
    }

    public String getCopyText() {
        return getReferringText();
    }

    public void setCopyText(String copyText) {
        setReferringText(copyText);
    }

    public String getAmendedText() {
        return amendedText;
    }

    public void setAmendedText(String amendedText) {
        this.amendedText = amendedText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Errata errata = (Errata) o;

        if (amendedText != null ? !amendedText.equals(errata.amendedText) : errata.amendedText != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (amendedText != null ? amendedText.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Errata{" +
                "copyText='" + getCopyText() + '\'' +
                "amendedText='" + amendedText + '\'' +
                '}';
    }
}
