package rosa.archive.model.aor;

import java.io.Serializable;

/**
 * Element representing an error in some text that a reader corrected.
 *
 * &lt;errata language copytext amendedtext id internal_ref /&gt;
 *
 * Attributes:
 * <ul>
 *   <li>language (required)</li>
 *   <li>copytext (required) : original text</li>
 *   <li>amendedtext (required) : corrected text from the reader</li>
 *   <li>id (optional)</li>
 *   <li>internal_ref (optional)</li>
 * </ul>
 *
 * Contains no elements.
 */
public class Errata extends Annotation implements Serializable {
    private static final long serialVersionUID = 1L;

    private String amendedText;

    public Errata() {
    }

    @Override
    public String toPrettyString() {
        return "Errata: " + getReferencedText() + " > " + amendedText;
    }

    public Errata(String id, String language, String copyText, String amendedText) {
        super(id, copyText, language, Location.INTEXT);
        this.amendedText = amendedText;
    }

    public String getAmendedText() {
        return amendedText;
    }

    public void setAmendedText(String amendedText) {
        this.amendedText = amendedText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;

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
        return "Errata{" + "copyText='" + getReferencedText() + '\'' + "amendedText='" + amendedText + '\'' + '}';
    }
}
