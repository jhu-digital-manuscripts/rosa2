package rosa.website.core.client.event;

import com.google.gwt.event.shared.GwtEvent;

public class BookSelectEvent extends GwtEvent<BookSelectEventHandler> {
    public static Type<BookSelectEventHandler> TYPE = new Type<>();

    private final boolean selected;
    private final String bookId;

    public BookSelectEvent(boolean selected, String bookId) {
        this.selected = selected;
        this.bookId = bookId;
    }

    public boolean isSelected() {
        return selected;
    }

    public String getBookId() {
        return bookId;
    }

    @Override
    public Type<BookSelectEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(BookSelectEventHandler handler) {
        handler.onBookSelect(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BookSelectEvent that = (BookSelectEvent) o;

        if (selected != that.selected) return false;
        return !(bookId != null ? !bookId.equals(that.bookId) : that.bookId != null);

    }

    @Override
    public int hashCode() {
        int result = (selected ? 1 : 0);
        result = 31 * result + (bookId != null ? bookId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "BookSelectEvent{" +
                "selected=" + selected +
                ", bookId='" + bookId + '\'' +
                '}';
    }
}
