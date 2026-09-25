package rosa.archive.model.aor;

import java.util.Objects;

/**
 * An annotation representing a numeral written on a page.
 */
public final class Numeral extends AbstractAnnotation {

    private String numeral;

    /**
     * Creates an empty numeral annotation.
     */
    public Numeral() {}

    /**
     * Creates a numeral annotation with the specified fields.
     *
     * @param id           the annotation identifier
     * @param referredText the text referred to by this numeral
     * @param numeral      the numeral value
     * @param language     the language code
     * @param location     the physical location on the page
     */
    public Numeral(String id, String referredText, String numeral, String language, Location location) {
        super(id, referredText, language, location);
        this.numeral = numeral;
    }

    /**
     * Returns the numeral value.
     *
     * @return the numeral string, or null
     */
    public String getNumeral() {
        return numeral;
    }

    /**
     * Sets the numeral value.
     *
     * @param numeral the numeral string
     */
    public void setNumeral(String numeral) {
        this.numeral = numeral;
    }

    @Override
    public String toPrettyString() {
        return "Numeral: (" + getReferencedText() + " " + numeral + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Numeral that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(numeral, that.numeral);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), numeral);
    }

    @Override
    public String toString() {
        return "Numeral{numeral='" + numeral + "'}";
    }
}
