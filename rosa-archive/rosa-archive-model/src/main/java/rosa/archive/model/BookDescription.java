package rosa.archive.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Prose description about a book.
 */
public final class BookDescription {

    /** Map of topics to text (note 'rend' to text content). */
    private Map<String, String> blocks;

    public BookDescription() {
        this.blocks = new HashMap<>();
    }

    public Map<String, String> getBlocks() {
        return blocks;
    }

    public void setBlocks(Map<String, String> blocks) {
        this.blocks = blocks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookDescription)) return false;

        BookDescription that = (BookDescription) o;

        return !(blocks != null ? !blocks.equals(that.blocks) : that.blocks != null);

    }

    @Override
    public int hashCode() {
        return blocks != null ? blocks.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "BookDescription{" +
                "blocks=" + blocks +
                '}';
    }
}
