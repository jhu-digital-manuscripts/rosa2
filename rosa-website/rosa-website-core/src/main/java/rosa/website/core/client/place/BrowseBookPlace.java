package rosa.website.core.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class BrowseBookPlace extends Place {

    private boolean useFlash;
    private String book;

    public BrowseBookPlace(boolean useFlash, String book) {
        this.useFlash = useFlash;
        this.book = book;
    }

    public boolean useFlash() {
        return useFlash;
    }

    public void setUseFlash(boolean useFlash) {
        this.useFlash = useFlash;
    }

    public String getBook() {
        return book;
    }

    @Prefix("browse")
    public static class Tokenizer implements PlaceTokenizer<BrowseBookPlace> {
        @Override
        public BrowseBookPlace getPlace(String token) {
            // TODO need app context to hold 'useFlash' state
            return new BrowseBookPlace(true, token);
        }

        @Override
        public String getToken(BrowseBookPlace place) {
            return place.getBook();
        }
    }

}
