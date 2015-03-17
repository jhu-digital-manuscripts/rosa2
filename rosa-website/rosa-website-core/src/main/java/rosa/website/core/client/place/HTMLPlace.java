package rosa.website.core.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

public class HTMLPlace extends Place {

    private final String name;

    public HTMLPlace(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static class Tokenizer implements PlaceTokenizer<HTMLPlace> {

        @Override
        public HTMLPlace getPlace(String token) {
            return new HTMLPlace(token);
        }

        @Override
        public String getToken(HTMLPlace place) {
            return place.getName();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HTMLPlace)) return false;

        HTMLPlace htmlPlace = (HTMLPlace) o;

        if (name != null ? !name.equals(htmlPlace.name) : htmlPlace.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "HTMLPlace{" +
                "name='" + name + '\'' +
                '}';
    }
}
