package rosa.archive.model.aor;

import java.util.Objects;

/**
 * An annotation representing a reader's correction of an error in the printed text.
 */
public final class Errata extends AbstractAnnotation {

    private String amendedText;

    /**
     * Creates an empty errata annotation.
     */
    public Errata() {}

    /**
     * Creates an errata annotation with the specified fields.
     *
     * @param id          the annotation identifier
     * @param language    the language code
     * @param copyText    the original (erroneous) text
     * @param amendedText the corrected text
     */
    public Errata(String id, String language, String copyText, String amendedText) {
        super(id, copyText, language, Location.INTEXT);
        this.amendedText = amendedText;
    }

    /**
     * Returns the corrected text.
     *
     * @return the amended text, or null
     */
    public String getAmendedText() {
        return amendedText;
    }

    /**
     * Sets the corrected text.
     *
     * @param amendedText the amended text
     */
    public void setAmendedText(String amendedText) {
        this.amendedText = amendedText;
    }

    @Override
    public String toPrettyString() {
        return "Errata: " + getReferencedText() + " > " + amendedText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Errata errata)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(amendedText, errata.amendedText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), amendedText);
    }

    @Override
    public String toString() {
        return "Errata{copyText='" + getReferencedText() + "', amendedText='" + amendedText + "'}";
    }
}
