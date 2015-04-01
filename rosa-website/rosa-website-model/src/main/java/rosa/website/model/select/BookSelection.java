package rosa.website.model.select;

import java.io.Serializable;

/**
 * Intermediate set in selecting a particular book to read. After a user has selected
 * a top level selection category, the data is sorted into a list of values of that
 * category. The number of books with that same value is counted.
 */
public class BookSelection implements Comparable<BookSelection>, Serializable {
    private static final long serialVersionUID = 1L;

    public SelectCategory category;
    public String name;
    private int count;

    public BookSelection() {
        count = 1;
    }

    /**
     * Create a new BookSelection
     *
     * @param category .
     * @param name .
     */
    public BookSelection(SelectCategory category, String name) {
        this.category = category;
        this.name = name;
        this.count = 1;
    }

    public int getCount() {
        return count;
    }

    /** Increment the number of times this name is present. */
    public void increment() {
        count++;
    }

    @Override
    public int compareTo(BookSelection o) {
        if (o == null || o.name == null) {
            return -1;
        }

        return name.compareToIgnoreCase(o.name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookSelection)) return false;

        BookSelection that = (BookSelection) o;

        if (count != that.count) return false;
        if (category != that.category) return false;
        return !(name != null ? !name.equals(that.name) : that.name != null);

    }

    @Override
    public int hashCode() {
        int result = category != null ? category.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + count;
        return result;
    }

    @Override
    public String toString() {
        return "BookSelection{" +
                "category=" + category +
                ", name='" + name + '\'' +
                ", count=" + count +
                '}';
    }
}
