package rosa.website.core.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class BookDescriptionPlace extends Place {
    private final String book;

    /**
     * @param book book ID
     */
    public BookDescriptionPlace(String book) {
        this.book = book;
    }

    public String getBook() {
        return book;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookDescriptionPlace)) return false;

        BookDescriptionPlace that = (BookDescriptionPlace) o;

        return !(book != null ? !book.equals(that.book) : that.book != null);

    }

    @Override
    public int hashCode() {
        return book != null ? book.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "BookDescriptionPlace{" +
                ", book='" + book + '\'' +
                '}';
    }

    @Prefix("book")
    public static class Tokenizer implements PlaceTokenizer<BookDescriptionPlace> {
        @Override
        public BookDescriptionPlace getPlace(String token) {
            return new BookDescriptionPlace(token);
        }

        @Override
        public String getToken(BookDescriptionPlace place) {
            return place.getBook();
        }
    }
}
