package rosa.archive.model.aor;

import java.util.Objects;

/**
 * An annotation representing a mark (such as a bracket, asterisk, or other symbol)
 * placed near text on a page.
 */
public final class Mark extends AbstractAnnotation {

    private String name;
    private String method;
    private String color;

    /**
     * Creates an empty mark annotation.
     */
    public Mark() {}

    /**
     * Creates a mark annotation with the specified fields.
     *
     * @param id           the annotation identifier
     * @param referredText the text near which the mark is placed
     * @param name         the mark name
     * @param method       the method used to make the mark
     * @param language     the language code
     * @param location     the physical location on the page
     */
    public Mark(String id, String referredText, String name, String method, String language, Location location) {
        super(id, referredText, language, location);
        this.name = name;
        this.method = method;
    }

    /**
     * Returns the mark name.
     *
     * @return the name, or null
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the mark name.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the method used to make the mark.
     *
     * @return the method, or null
     */
    public String getMethod() {
        return method;
    }

    /**
     * Sets the method.
     *
     * @param method the method
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * Returns the mark color.
     *
     * @return the color, or null
     */
    public String getColor() {
        return color;
    }

    /**
     * Sets the color.
     *
     * @param color the color
     */
    public void setColor(String color) {
        this.color = color;
    }

    @Override
    public String toPrettyString() {
        return "Mark: " + name + " (" + getReferencedText() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Mark mark)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(name, mark.name) &&
                Objects.equals(method, mark.method) &&
                Objects.equals(color, mark.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, method, color);
    }

    @Override
    public String toString() {
        return "Mark{name='" + name + "', method='" + method + "', color='" + color + "'}";
    }
}
