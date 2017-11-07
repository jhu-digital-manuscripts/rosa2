package rosa.archive.model.aor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Element representing a drawing on a page.
 *
 * &lt;drawing type book_orientation place method colour id anchor_text&gt;
 *   &lt;text /&gt;
 *   &lt;person /&gt;
 *   &lt;book /&gt;
 *   &lt;location /&gt;
 *   &lt;symbol_in_text /&gt;
 *   &lt;internal_ref&gt;
 *   &lt;translation /&gt;
 * &lt;/drawing&gt;
 *
 * Attributes:
 * <ul>
 *   <li>type (required)</li>
 *   <li>book_orientation (required:number)</li>
 *   <li>place (required)</li>
 *   <li>method (required)</li>
 *   <li>colour (optional)</li>
 *   <li>id (optional)</li>
 *   <li>anchor_text (optional)</li>
 * </ul>
 *
 * Contains elements:
 * <ul>
 *   <li>text (zero or more)</li>
 *   <li>person (zero or more)</li>
 *   <li>book (zero or more)</li>
 *   <li>location (zero or more)</li>
 *   <li>symbol_in_text (zero or more)</li>
 *   <li>internal_ref (zero or more) : {@link InternalReference}</li>
 *   <li>translation (zero or one)</li>
 * </ul>
 */
public class Drawing extends Annotation implements Serializable {
    private static final long serialVersionUID = 1L;

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

    public Drawing() {
        this.texts = new ArrayList<>();
        this.people = new ArrayList<>();
        this.books = new ArrayList<>();
        this.locations = new ArrayList<>();
        this.symbols = new ArrayList<>();
    }

    public Drawing(String id, String referringText, Location location, String name, String method, String language) {
        super(id, referringText, language, location);
        this.method = method;
        this.texts = new ArrayList<>();
        this.people = new ArrayList<>();
        this.books = new ArrayList<>();
        this.locations = new ArrayList<>();
        this.symbols = new ArrayList<>();
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOrientation() {
        return orientation;
    }

    public void setOrientation(String orientation) {
        this.orientation = orientation;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public List<TextEl> getTexts() {
        return texts;
    }

    public void setTexts(List<TextEl> texts) {
        this.texts = texts;
    }

    public List<String> getPeople() {
        return people;
    }

    public void setPeople(List<String> people) {
        this.people = people;
    }

    public List<String> getBooks() {
        return books;
    }

    public void setBooks(List<String> books) {
        this.books = books;
    }

    public List<String> getLocations() {
        return locations;
    }

    public void setLocations(List<String> locations) {
        this.locations = locations;
    }

    public List<String> getSymbols() {
        return symbols;
    }

    public void setSymbols(List<String> symbols) {
        this.symbols = symbols;
    }

    public List<InternalReference> getInternalRefs() {
        return internalRefs;
    }

    public void setInternalRefs(List<InternalReference> internalRefs) {
        this.internalRefs = internalRefs;
    }

    public String getTranslation() {
        return translation;
    }

    public void setTranslation(String translation) {
        this.translation = translation;
    }

    @Override
    public String toPrettyString() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Drawing drawing = (Drawing) o;

        if (method != null ? !method.equals(drawing.method) : drawing.method != null) return false;
        if (color != null ? !color.equals(drawing.color) : drawing.color != null) return false;
        if (type != null ? !type.equals(drawing.type) : drawing.type != null) return false;
        if (orientation != null ? !orientation.equals(drawing.orientation) : drawing.orientation != null) return false;
        if (texts != null ? !texts.equals(drawing.texts) : drawing.texts != null) return false;
        if (people != null ? !people.equals(drawing.people) : drawing.people != null) return false;
        if (books != null ? !books.equals(drawing.books) : drawing.books != null) return false;
        if (locations != null ? !locations.equals(drawing.locations) : drawing.locations != null) return false;
        if (symbols != null ? !symbols.equals(drawing.symbols) : drawing.symbols != null) return false;
        if (internalRefs != null ? !internalRefs.equals(drawing.internalRefs) : drawing.internalRefs != null)
            return false;
        return translation != null ? translation.equals(drawing.translation) : drawing.translation == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (method != null ? method.hashCode() : 0);
        result = 31 * result + (color != null ? color.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (orientation != null ? orientation.hashCode() : 0);
        result = 31 * result + (texts != null ? texts.hashCode() : 0);
        result = 31 * result + (people != null ? people.hashCode() : 0);
        result = 31 * result + (books != null ? books.hashCode() : 0);
        result = 31 * result + (locations != null ? locations.hashCode() : 0);
        result = 31 * result + (symbols != null ? symbols.hashCode() : 0);
        result = 31 * result + (internalRefs != null ? internalRefs.hashCode() : 0);
        result = 31 * result + (translation != null ? translation.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Drawing{" +
                "method='" + method + '\'' +
                ", color='" + color + '\'' +
                ", type='" + type + '\'' +
                ", orientation='" + orientation + '\'' +
                ", texts=" + texts +
                ", people=" + people +
                ", books=" + books +
                ", locations=" + locations +
                ", symbols=" + symbols +
                ", internalRefs=" + internalRefs +
                ", translation='" + translation + '\'' +
                super.toString() + '}';
    }
}
