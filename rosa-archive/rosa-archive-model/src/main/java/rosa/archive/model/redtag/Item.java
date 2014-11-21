package rosa.archive.model.redtag;

import java.io.Serializable;

/**
 * An item on a page in a book.
 *
 * This item takes the place of zero or more lines of text on the page. This information is used
 * to help determine the number of lines of text are on a page.
 */
public class Item implements Serializable {
    private static final long serialVersionUID = 1L;

    private int lines;

    public Item() {
        this.lines = -1;
    }

    public int getLines() {
        return lines;
    }

    public void setLines(int lines) {
        this.lines = lines;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Item item = (Item) o;

        if (lines != item.lines) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return lines;
    }

    @Override
    public String toString() {
        return "Item{" +
                "lines=" + lines +
                '}';
    }
}
