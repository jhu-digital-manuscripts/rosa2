package rosa.archive.model.aor;

import java.util.Objects;

/**
 * An annotation representing a symbol drawn in the text of a page.
 */
public final class Symbol extends AbstractAnnotation {

    private String name;

    /**
     * Creates an empty symbol annotation.
     */
    public Symbol() {}

    /**
     * Creates a symbol annotation with the specified fields.
     *
     * @param id           the annotation identifier
     * @param referredText the text near which the symbol is placed
     * @param name         the symbol name
     * @param language     the language code
     * @param location     the physical location on the page
     */
    public Symbol(String id, String referredText, String name, String language, Location location) {
        super(id, referredText, language, location);
        this.name = name;
    }

    /**
     * Returns the symbol name.
     *
     * @return the name, or null
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the symbol name.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toPrettyString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Symbol symbol)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(name, symbol.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name);
    }

    @Override
    public String toString() {
        return "Symbol{name='" + name + "'}";
    }
}
