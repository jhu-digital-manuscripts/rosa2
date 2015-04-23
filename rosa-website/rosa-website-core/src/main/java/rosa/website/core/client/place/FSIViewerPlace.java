package rosa.website.core.client.place;

import com.google.gwt.place.shared.Place;

public class FSIViewerPlace extends Place {

    private String type;
    private String book;

    /**
     * @param type viewer type (pages, showcase)
     * @param book name of book
     */
    public FSIViewerPlace(String type, String book) {
        this.type = type;
        this.book = book;
    }

    public String getBook() {
        return book;
    }

    public String getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FSIViewerPlace that = (FSIViewerPlace) o;

        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        return !(book != null ? !book.equals(that.book) : that.book != null);

    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (book != null ? book.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FSIViewerPlace{" +
                "type='" + type + '\'' +
                ", book='" + book + '\'' +
                '}';
    }
}
