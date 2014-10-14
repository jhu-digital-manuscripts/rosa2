package rosa.archive.model.aor;

import java.util.ArrayList;
import java.util.List;

public class Position {

    String text;
    String place;
    int orientation;
    List<String> people;
    List<String> books;
    List<String> locations;
    List<Underline> emphasis;

    public Position() {
        people = new ArrayList<>();
        books = new ArrayList<>();
        locations = new ArrayList<>();
        emphasis = new ArrayList<>();
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
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
        if (text != null ? !text.equals(position.text) : position.text != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = text != null ? text.hashCode() : 0;
        result = 31 * result + (place != null ? place.hashCode() : 0);
        result = 31 * result + orientation;
        result = 31 * result + (people != null ? people.hashCode() : 0);
        result = 31 * result + (books != null ? books.hashCode() : 0);
        result = 31 * result + (locations != null ? locations.hashCode() : 0);
        result = 31 * result + (emphasis != null ? emphasis.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Position{" +
                "text='" + text + '\'' +
                ", place='" + place + '\'' +
                ", orientation=" + orientation +
                ", people=" + people +
                ", books=" + books +
                ", locations=" + locations +
                ", emphasis=" + emphasis +
                '}';
    }
}