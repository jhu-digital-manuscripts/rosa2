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

}
