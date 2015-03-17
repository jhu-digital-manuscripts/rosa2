package rosa.website.core.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

public class CSVDataPlace extends Place {
    private final String name;
    private final String collection;

    public CSVDataPlace(String name, String collection) {
        this.name = name;
        this.collection = collection;
    }

    public String getName() {
        return name;
    }

    public String getCollection() {
        return collection;
    }

    public static class Tokenizer implements PlaceTokenizer<CSVDataPlace> {
        @Override
        public CSVDataPlace getPlace(String token) {
            // COLLECTION;NAME
            String[] parts = token.split(";");
            return new CSVDataPlace(parts[1], parts[0]);
        }

        @Override
        public String getToken(CSVDataPlace place) {
            return place.getName();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CSVDataPlace)) return false;

        CSVDataPlace that = (CSVDataPlace) o;

        if (collection != null ? !collection.equals(that.collection) : that.collection != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (collection != null ? collection.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "CSVDataPlace{" +
                "name='" + name + '\'' +
                ", collection='" + collection + '\'' +
                '}';
    }
}
