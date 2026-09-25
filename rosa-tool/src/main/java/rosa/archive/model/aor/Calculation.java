package rosa.archive.model.aor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An annotation representing a mathematical calculation on a page.
 */
public final class Calculation extends AbstractAnnotation {

    private String type;
    private int orientation;
    private String method;
    private String content;
    private List<String> data;

    /**
     * Creates a calculation annotation with the specified fields.
     *
     * @param id          the annotation identifier
     * @param type        the calculation type
     * @param orientation the book orientation (degrees)
     * @param location    the physical location on the page
     */
    public Calculation(String id, String type, int orientation, Location location) {
        this(id, type, orientation, location, null, null);
    }

    /**
     * Creates a calculation annotation with full details.
     *
     * @param id          the annotation identifier
     * @param type        the calculation type
     * @param orientation the book orientation (degrees)
     * @param location    the physical location on the page
     * @param method      the calculation method
     * @param internalRef an internal reference identifier
     */
    public Calculation(String id, String type, int orientation, Location location, String method, String internalRef) {
        super(id, null, null, location);
        setInternalRef(internalRef);
        this.type = type;
        this.orientation = orientation;
        this.method = method;
        this.data = new ArrayList<>();
    }

    /**
     * Returns the calculation type.
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the calculation type.
     *
     * @param type the type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns the book orientation in degrees.
     *
     * @return the orientation
     */
    public int getOrientation() {
        return orientation;
    }

    /**
     * Sets the book orientation.
     *
     * @param orientation the orientation in degrees
     */
    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    /**
     * Returns the calculation method.
     *
     * @return the method, or null
     */
    public String getMethod() {
        return method;
    }

    /**
     * Sets the calculation method.
     *
     * @param method the method
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * Returns the calculation data elements.
     *
     * @return the data list (never null)
     */
    public List<String> getData() {
        return data;
    }

    /**
     * Sets the calculation data elements.
     *
     * @param data the data list
     */
    public void setData(List<String> data) {
        this.data = data;
    }

    /**
     * Adds a data element to the calculation.
     *
     * @param item the data element to add
     */
    public void addData(String item) {
        if (this.data != null) {
            this.data.add(item);
        }
    }

    /**
     * Returns the content text of the calculation.
     *
     * @return the content, or null
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the content text.
     *
     * @param content the content
     */
    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toPrettyString() {
        StringBuilder d = new StringBuilder();
        data.forEach(d::append);
        return (content != null ? "<p>" + content + "</p>" : "") +
                (!d.isEmpty() ? "<p>" + d + "</p>" : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Calculation that)) return false;
        if (!super.equals(o)) return false;
        return orientation == that.orientation &&
                Objects.equals(type, that.type) &&
                Objects.equals(method, that.method) &&
                Objects.equals(content, that.content) &&
                Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), type, orientation, method, content, data);
    }

    @Override
    public String toString() {
        return "Calculation{" +
                "type='" + type + '\'' +
                ", orientation=" + orientation +
                ", method='" + method + '\'' +
                ", content='" + content + '\'' +
                ", data=" + data +
                '}';
    }
}
