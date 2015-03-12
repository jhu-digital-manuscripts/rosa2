package rosa.website.core.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

public class CSVDataPlace extends Place {
    private final String name;

    public CSVDataPlace(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static class Tokenizer implements PlaceTokenizer<CSVDataPlace> {
        @Override
        public CSVDataPlace getPlace(String token) {
            return new CSVDataPlace(token);
        }

        @Override
        public String getToken(CSVDataPlace place) {
            return place.getName();
        }
    }

}
