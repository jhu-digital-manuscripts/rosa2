package rosa.website.model.select;

import java.io.Serializable;

/**
 * Contains specific book information. Bottom level node used in the CellBrowser
 * for selecting books.
 */
public final class BookInfo implements Comparable<BookInfo>, Serializable {
    private static final long serialVersionUID = 1L;

    public String title;
    public String id;

    public BookInfo() {}

    public BookInfo(String title, String id) {
        this.title = title;
        this.id  = id;
    }

    @Override
    public int compareTo(BookInfo o) {
        return o == null || o.title == null ? -1 : title.compareToIgnoreCase(o.title);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookInfo)) return false;

        BookInfo bookInfo = (BookInfo) o;

        if (title != null ? !title.equals(bookInfo.title) : bookInfo.title != null) return false;
        return !(id != null ? !id.equals(bookInfo.id) : bookInfo.id != null);

    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "BookInfo{" +
                "title='" + title + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}
