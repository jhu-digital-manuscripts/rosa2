package rosa.archive.model.aor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Position implements Serializable {
    private static final long serialVersionUID = 1L;

    private Location place;
    private int orientation;
    private List<String> texts;
    private List<String> symbols;
    private List<String> people;
    private List<String> books;
    private List<String> locations;
    private List<XRef> xRefs;
    private List<Underline> emphasis;
    /** References pulled out from the marginalia */
    private List<InternalReference> internalRefs;
    /** References embedded in <marginalia_text> elements, serialized differently than <internal_ref> */
    private List<InternalReference> marginaliaRefs;

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

    public List<String> getTexts() {
        return texts;
    }

    public void setTexts(List<String> texts) {
        this.texts = texts;
    }

    public List<String> getSymbols() {
        return symbols;
    }

    public void setSymbols(List<String> symbols) {
        this.symbols = symbols;
    }

    public Location getPlace() {
        return place;
    }

    public void setPlace(Location place) {
        this.place = place;
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
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

    public List<Underline> getEmphasis() {
        return emphasis;
    }

    public void setEmphasis(List<Underline> emphasis) {
        this.emphasis = emphasis;
    }

    public List<XRef> getxRefs() {
        return xRefs;
    }

    public void setxRefs(List<XRef> xRefs) {
        this.xRefs = xRefs;
    }

    public List<InternalReference> getInternalRefs() {
        return internalRefs;
    }

    public void setInternalRefs(List<InternalReference> internalRefs) {
        this.internalRefs = internalRefs;
    }

    public List<InternalReference> getMarginaliaRefs() {
        return marginaliaRefs;
    }

    public void setMarginaliaRefs(List<InternalReference> marginaliaRefs) {
        this.marginaliaRefs = marginaliaRefs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Position position = (Position) o;
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
        return Objects.hash(place, orientation, texts, symbols, people, books, locations, xRefs, emphasis, internalRefs, marginaliaRefs);
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
                ", xRefs=" + xRefs +
                ", emphasis=" + emphasis +
                ", internalRefs=" + internalRefs +
                ", marginaliaRefs=" + marginaliaRefs +
                '}';
    }
}