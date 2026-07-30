package rosa.archive.model.aor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An annotation representing a drawing on a page.
 *
 * <p>Drawings may reference people, books, locations, and symbols, and include
 * text elements and translations.</p>
 */
public final class Drawing extends AbstractAnnotation {

    private String method;
    private String color;
    private String type;
    private String orientation;
    private List<TextEl> texts;
    private List<String> people;
    private List<String> books;
    private List<String> locations;
    private List<String> symbols;
    private List<InternalReference> internalRefs;
    private String translation;

    /**
     * Creates an empty drawing annotation.
     */
    public Drawing() {
        this.texts = new ArrayList<>();
        this.people = new ArrayList<>();
        this.books = new ArrayList<>();
        this.locations = new ArrayList<>();
        this.symbols = new ArrayList<>();
        this.internalRefs = new ArrayList<>();
    }

    /**
     * Creates a drawing annotation with the specified fields.
     *
     * @param id           the annotation identifier
     * @param referredText the text referred to by this drawing
     * @param location     the physical location on the page
     * @param name         the drawing type name
     * @param method       the drawing method
     * @param language     the language code
     */
    public Drawing(String id, String referredText, Location location, String name, String method, String language) {
        super(id, referredText, language, location);
        this.method = method;
        this.type = name;
        this.texts = new ArrayList<>();
        this.people = new ArrayList<>();
        this.books = new ArrayList<>();
        this.locations = new ArrayList<>();
        this.symbols = new ArrayList<>();
        this.internalRefs = new ArrayList<>();
    }

    /**
     * Returns the drawing method.
     *
     * @return the method, or null
     */
    public String getMethod() {
        return method;
    }

    /**
     * Sets the drawing method.
     *
     * @param method the method
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * Returns the drawing type.
     *
     * @return the type, or null
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the drawing type.
     *
     * @param type the type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns the book orientation for this drawing.
     *
     * @return the orientation, or null
     */
    public String getOrientation() {
        return orientation;
    }

    /**
     * Sets the book orientation.
     *
     * @param orientation the orientation
     */
    public void setOrientation(String orientation) {
        this.orientation = orientation;
    }

    /**
     * Returns the color of this drawing.
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

    /**
     * Returns the text elements within this drawing.
     *
     * @return the text elements (never null)
     */
    public List<TextEl> getTexts() {
        return texts;
    }

    /**
     * Sets the text elements.
     *
     * @param texts the text elements
     */
    public void setTexts(List<TextEl> texts) {
        this.texts = texts;
    }

    /**
     * Returns the people referenced by this drawing.
     *
     * @return the people list (never null)
     */
    public List<String> getPeople() {
        return people;
    }

    /**
     * Sets the people list.
     *
     * @param people the people list
     */
    public void setPeople(List<String> people) {
        this.people = people;
    }

    /**
     * Returns the books referenced by this drawing.
     *
     * @return the books list (never null)
     */
    public List<String> getBooks() {
        return books;
    }

    /**
     * Sets the books list.
     *
     * @param books the books list
     */
    public void setBooks(List<String> books) {
        this.books = books;
    }

    /**
     * Returns the locations referenced by this drawing.
     *
     * @return the locations list (never null)
     */
    public List<String> getLocations() {
        return locations;
    }

    /**
     * Sets the locations list.
     *
     * @param locations the locations list
     */
    public void setLocations(List<String> locations) {
        this.locations = locations;
    }

    /**
     * Returns the symbols referenced by this drawing.
     *
     * @return the symbols list (never null)
     */
    public List<String> getSymbols() {
        return symbols;
    }

    /**
     * Sets the symbols list.
     *
     * @param symbols the symbols list
     */
    public void setSymbols(List<String> symbols) {
        this.symbols = symbols;
    }

    /**
     * Returns the internal references.
     *
     * @return the internal references (never null)
     */
    public List<InternalReference> getInternalRefs() {
        return internalRefs;
    }

    /**
     * Sets the internal references.
     *
     * @param internalRefs the internal references
     */
    public void setInternalRefs(List<InternalReference> internalRefs) {
        this.internalRefs = internalRefs;
    }

    /**
     * Returns the translation of this drawing's text.
     *
     * @return the translation, or null
     */
    public String getTranslation() {
        return translation;
    }

    /**
     * Sets the translation.
     *
     * @param translation the translation
     */
    public void setTranslation(String translation) {
        this.translation = translation;
    }

    @Override
    public String toPrettyString() {
        return "Drawing{type=" + type + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Drawing drawing)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(method, drawing.method) &&
                Objects.equals(color, drawing.color) &&
                Objects.equals(type, drawing.type) &&
                Objects.equals(orientation, drawing.orientation) &&
                Objects.equals(texts, drawing.texts) &&
                Objects.equals(people, drawing.people) &&
                Objects.equals(books, drawing.books) &&
                Objects.equals(locations, drawing.locations) &&
                Objects.equals(symbols, drawing.symbols) &&
                Objects.equals(internalRefs, drawing.internalRefs) &&
                Objects.equals(translation, drawing.translation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), method, color, type, orientation,
                texts, people, books, locations, symbols, internalRefs, translation);
    }

    @Override
    public String toString() {
        return "Drawing{" +
                "method='" + method + '\'' +
                ", color='" + color + '\'' +
                ", type='" + type + '\'' +
                ", orientation='" + orientation + '\'' +
                ", texts=" + texts.size() +
                ", people=" + people.size() +
                ", books=" + books.size() +
                ", locations=" + locations.size() +
                ", symbols=" + symbols.size() +
                ", translation='" + translation + '\'' +
                '}';
    }
}
