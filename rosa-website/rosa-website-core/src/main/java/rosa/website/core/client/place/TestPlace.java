package rosa.website.core.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

public class TestPlace extends Place {
    private String name;

    public TestPlace(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static class Tokenizer implements PlaceTokenizer<TestPlace> {
        @Override
        public TestPlace getPlace(String token) {
            return new TestPlace(token);
        }

        @Override
        public String getToken(TestPlace place) {
            return "";
        }
    }
}
