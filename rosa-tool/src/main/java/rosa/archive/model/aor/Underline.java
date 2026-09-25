package rosa.archive.model.aor;

import java.util.Objects;

/**
 * An annotation representing underlined text on a page.
 */
public final class Underline extends AbstractAnnotation {

    private String method;
    private String type;
    private String color;

    /**
     * Creates an empty underline annotation.
     */
    public Underline() {}

    /**
     * Creates an underline annotation with location defaulting to INTEXT.
     *
     * @param id           the annotation identifier
     * @param referredText the underlined text
     * @param method       the method used
     * @param type         the underline type
     * @param language     the language code
     */
    public Underline(String id, String referredText, String method, String type, String language) {
        this(id, referredText, method, type, language, Location.INTEXT);
    }

    /**
     * Creates an underline annotation with a specified location.
     *
     * @param id           the annotation identifier
     * @param referredText the underlined text
     * @param method       the method used
     * @param type         the underline type
     * @param language     the language code
     * @param location     the physical location on the page
     */
    public Underline(String id, String referredText, String method, String type, String language, Location location) {
        super(id, referredText, language, location);
        this.method = method;
        this.type = type;
    }

    /**
     * Returns the underline method.
     *
     * @return the method, or null
     */
    public String getMethod() {
        return method;
    }

    /**
     * Sets the underline method.
     *
     * @param method the method
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * Returns the underline type.
     *
     * @return the type, or null
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the underline type.
     *
     * @param type the type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns the underline color.
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
        return "Underline (" + getReferencedText() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Underline underline)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(method, underline.method) &&
                Objects.equals(type, underline.type) &&
                Objects.equals(color, underline.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), method, type, color);
    }

    @Override
    public String toString() {
        return "Underline{method='" + method + "', type='" + type + "', color='" + color + "'}";
    }
}
