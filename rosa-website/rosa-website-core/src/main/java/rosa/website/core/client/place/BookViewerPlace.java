package rosa.website.core.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

public class BookViewerPlace extends Place {

    private String type;
    private String book;

    public BookViewerPlace(String type, String book) {
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
    public static class Tokenizer implements PlaceTokenizer<BookViewerPlace> {
        @Override
        public BookViewerPlace getPlace(String token) {
            return new BookViewerPlace("browse", token);
        }

        @Override
        public String getToken(BookViewerPlace place) {
            return place.getBook();
        }
    }

}
