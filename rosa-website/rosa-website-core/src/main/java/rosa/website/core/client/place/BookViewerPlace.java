package rosa.website.core.client.place;

import com.google.gwt.place.shared.Place;

public class BookViewerPlace extends Place {

    private final String type;
    private final String book;
    private final String page;

    /**
     * @param type viewer type (pages, showcase)
     * @param book name of book
     */
    public BookViewerPlace(String type, String book) {
        this(type, book, null);
    }

    /**
     * @param type viewer type
     * @param book book id
     * @param page page id
     */
    public BookViewerPlace(String type, String book, String page) {
        this.type = type;
        this.book = book;
        this.page = page;
    }

    public String getBook() {
        return book;
    }

    public String getType() {
        return type;
    }

    public String getPage() {
        return page;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BookViewerPlace that = (BookViewerPlace) o;

        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        if (book != null ? !book.equals(that.book) : that.book != null) return false;
        return !(page != null ? !page.equals(that.page) : that.page != null);

    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (book != null ? book.hashCode() : 0);
        result = 31 * result + (page != null ? page.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "BookViewerPlace{" +
                "type='" + type + '\'' +
                ", book='" + book + '\'' +
                ", page='" + page + '\'' +
                '}';
    }
}
