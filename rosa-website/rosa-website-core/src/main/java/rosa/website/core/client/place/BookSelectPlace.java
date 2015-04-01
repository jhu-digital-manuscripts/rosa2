package rosa.website.core.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import rosa.website.model.select.SelectCategory;

public class BookSelectPlace extends Place {

    private final SelectCategory category;

    public BookSelectPlace(SelectCategory category) {
        this.category = category;
    }

    public SelectCategory getCategory() {
        return category;
    }

    @Prefix("select")
    public static class Tokenizer implements PlaceTokenizer<BookSelectPlace> {
        @Override
        public BookSelectPlace getPlace(String token) {
            if (token.endsWith(":") || token.endsWith(";")) {
                token = token.substring(0, token.length() - 1);
            }

            return new BookSelectPlace(SelectCategory.valueOf(token));
        }

        @Override
        public String getToken(BookSelectPlace place) {
            return place.getCategory().toString();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookSelectPlace)) return false;

        BookSelectPlace that = (BookSelectPlace) o;

        return category == that.category;

    }

    @Override
    public int hashCode() {
        return category != null ? category.hashCode() : 0;
    }
}
