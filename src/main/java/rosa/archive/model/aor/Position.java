package rosa.archive.model.aor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A positional entry within a marginalia language section, describing text and references
 * at a specific location on the page.
 */
public final class Position {

    private Location place;
    private int orientation;
    private List<String> texts;
    private List<String> symbols;
    private List<String> people;
    private List<String> books;
    private List<String> locations;
    private List<XRef> xRefs;
    private List<Underline> emphasis;
    private List<InternalReference> internalRefs;
    private List<InternalReference> marginaliaRefs;

    /**
     * Creates an empty position.
     */
    public Position() {
        texts = new ArrayList<>();
        symbols = new ArrayList<>();
        people = new ArrayList<>();
        books = new ArrayList<>();
        locations = new ArrayList<>();
        xRefs = new ArrayList<>();
        emphasis = new ArrayList<>();
        internalRefs = new ArrayList<>();
        marginaliaRefs = new ArrayList<>();
    }

    /**
     * Returns the physical placement on the page.
     *
     * @return the place, or null
     */
    public Location getPlace() {
        return place;
    }

    /**
     * Sets the physical placement.
     *
     * @param place the place
     */
    public void setPlace(Location place) {
        this.place = place;
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
     * Returns the text entries at this position.
     *
     * @return the texts (never null)
     */
    public List<String> getTexts() {
        return texts;
    }

    /**
     * Sets the text entries.
     *
     * @param texts the texts
     */
    public void setTexts(List<String> texts) {
        this.texts = texts;
    }

    /**
     * Returns the symbols referenced at this position.
     *
     * @return the symbols (never null)
     */
    public List<String> getSymbols() {
        return symbols;
    }

    /**
     * Sets the symbols.
     *
     * @param symbols the symbols
     */
    public void setSymbols(List<String> symbols) {
        this.symbols = symbols;
    }

    /**
     * Returns the people referenced at this position.
     *
     * @return the people (never null)
     */
    public List<String> getPeople() {
        return people;
    }

    /**
     * Sets the people.
     *
     * @param people the people
     */
    public void setPeople(List<String> people) {
        this.people = people;
    }

    /**
     * Returns the books referenced at this position.
     *
     * @return the books (never null)
     */
    public List<String> getBooks() {
        return books;
    }

    /**
     * Sets the books.
     *
     * @param books the books
     */
    public void setBooks(List<String> books) {
        this.books = books;
    }

    /**
     * Returns the locations referenced at this position.
     *
     * @return the locations (never null)
     */
    public List<String> getLocations() {
        return locations;
    }

    /**
     * Sets the locations.
     *
     * @param locations the locations
     */
    public void setLocations(List<String> locations) {
        this.locations = locations;
    }

    /**
     * Returns the cross-references at this position.
     *
     * @return the cross-references (never null)
     */
    public List<XRef> getXRefs() {
        return xRefs;
    }

    /**
     * Sets the cross-references.
     *
     * @param xRefs the cross-references
     */
    public void setXRefs(List<XRef> xRefs) {
        this.xRefs = xRefs;
    }

    /**
     * Returns the emphasis (underline) elements at this position.
     *
     * @return the emphasis list (never null)
     */
    public List<Underline> getEmphasis() {
        return emphasis;
    }

    /**
     * Sets the emphasis elements.
     *
     * @param emphasis the emphasis list
     */
    public void setEmphasis(List<Underline> emphasis) {
        this.emphasis = emphasis;
    }

    /**
     * Returns the internal references pulled from the marginalia.
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
     * Returns the references embedded in marginalia_text elements.
     *
     * @return the marginalia references (never null)
     */
    public List<InternalReference> getMarginaliaRefs() {
        return marginaliaRefs;
    }

    /**
     * Sets the marginalia references.
     *
     * @param marginaliaRefs the marginalia references
     */
    public void setMarginaliaRefs(List<InternalReference> marginaliaRefs) {
        this.marginaliaRefs = marginaliaRefs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Position position)) return false;
        return orientation == position.orientation &&
                place == position.place &&
                Objects.equals(texts, position.texts) &&
                Objects.equals(symbols, position.symbols) &&
                Objects.equals(people, position.people) &&
                Objects.equals(books, position.books) &&
                Objects.equals(locations, position.locations) &&
                Objects.equals(xRefs, position.xRefs) &&
                Objects.equals(emphasis, position.emphasis) &&
                Objects.equals(internalRefs, position.internalRefs) &&
                Objects.equals(marginaliaRefs, position.marginaliaRefs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(place, orientation, texts, symbols, people, books,
                locations, xRefs, emphasis, internalRefs, marginaliaRefs);
    }

    @Override
    public String toString() {
        return "Position{" +
                "place=" + place +
                ", orientation=" + orientation +
                ", texts=" + texts +
                ", symbols=" + symbols +
                ", people=" + people +
                ", books=" + books +
                ", locations=" + locations +
                '}';
    }
}
