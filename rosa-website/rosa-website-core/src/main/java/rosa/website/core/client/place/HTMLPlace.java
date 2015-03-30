package rosa.website.core.client.place;

import com.google.gwt.place.shared.Place;

public class HTMLPlace extends Place {

    private final String collection;
    private final String name;

    public HTMLPlace(String collection, String name) {
        this.collection = collection;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getCollection() {
        return collection;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HTMLPlace)) return false;

        HTMLPlace htmlPlace = (HTMLPlace) o;

        if (collection != null ? !collection.equals(htmlPlace.collection) : htmlPlace.collection != null) return false;
        return !(name != null ? !name.equals(htmlPlace.name) : htmlPlace.name != null);

    }

    @Override
    public int hashCode() {
        int result = collection != null ? collection.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "HTMLPlace{" +
                "collection='" + collection + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
