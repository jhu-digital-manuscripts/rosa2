package rosa.website.core.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

public class BrowseBookPlace extends Place {

    private String type;
    private String book;

    public BrowseBookPlace(String type, String book) {
        this.type = type;
        this.book = book;
    }

    public String getBook() {
        return book;
    }

    public String getType() {
        return type;
    }

    //    @Prefix("browse")
    public static class Tokenizer implements PlaceTokenizer<BrowseBookPlace> {
        @Override
        public BrowseBookPlace getPlace(String token) {
            return new BrowseBookPlace("browse", token);
        }

        @Override
        public String getToken(BrowseBookPlace place) {
            return place.getBook();
        }
    }

}
