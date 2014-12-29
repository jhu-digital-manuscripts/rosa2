package rosa.archive.model.aor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Position implements Serializable {
    private static final long serialVersionUID = 1L;

    Location place;
    int orientation;
    List<String> texts;
    List<String> people;
    List<String> books;
    List<String> locations;
    List<XRef> xRefs;
    List<Underline> emphasis;

    public Position() {
        texts = new ArrayList<>();
        people = new ArrayList<>();
        books = new ArrayList<>();
        locations = new ArrayList<>();
        xRefs = new ArrayList<>();
        emphasis = new ArrayList<>();
    }

    public List<String> getTexts() {
        return texts;
    }

    public void setTexts(List<String> texts) {
        this.texts = texts;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Position position = (Position) o;

        if (orientation != position.orientation) return false;
        if (books != null ? !books.equals(position.books) : position.books != null) return false;
        if (emphasis != null ? !emphasis.equals(position.emphasis) : position.emphasis != null) return false;
        if (locations != null ? !locations.equals(position.locations) : position.locations != null) return false;
        if (people != null ? !people.equals(position.people) : position.people != null) return false;
        if (place != null ? !place.equals(position.place) : position.place != null) return false;
        if (texts != null ? !texts.equals(position.texts) : position.texts != null) return false;
        if (xRefs != null ? !xRefs.equals(position.xRefs) : position.xRefs != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = place != null ? place.hashCode() : 0;
        result = 31 * result + orientation;
        result = 31 * result + (texts != null ? texts.hashCode() : 0);
        result = 31 * result + (people != null ? people.hashCode() : 0);
        result = 31 * result + (books != null ? books.hashCode() : 0);
        result = 31 * result + (locations != null ? locations.hashCode() : 0);
        result = 31 * result + (xRefs != null ? xRefs.hashCode() : 0);
        result = 31 * result + (emphasis != null ? emphasis.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Position{" +
                "place='" + place + '\'' +
                ", orientation=" + orientation +
                ", texts=" + texts +
                ", people=" + people +
                ", books=" + books +
                ", locations=" + locations +
                ", xRefs=" + xRefs +
                ", emphasis=" + emphasis +
                '}';
    }
}