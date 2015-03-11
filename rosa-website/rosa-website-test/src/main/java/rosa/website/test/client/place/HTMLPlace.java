package rosa.website.test.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class HTMLPlace extends Place {

    private final String name;

    public HTMLPlace(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Prefix("home")
    public static class Tokenizer implements PlaceTokenizer<HTMLPlace> {

        @Override
        public HTMLPlace getPlace(String token) {
            return new HTMLPlace("home");
        }

        @Override
        public String getToken(HTMLPlace place) {
            return "";
        }
    }

//    @Prefix("notHome")
//    public class NotHomeTokenizer implements PlaceTokenizer<HTMLPlace> {
//        @Override
//        public HTMLPlace getPlace(String token) {
//            return new HTMLPlace("notHome");
//        }
//
//        @Override
//        public String getToken(HTMLPlace place) {
//            return "";
//        }
//    }
}
