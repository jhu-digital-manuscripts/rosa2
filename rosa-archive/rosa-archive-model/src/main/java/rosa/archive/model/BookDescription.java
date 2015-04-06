package rosa.archive.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Prose description about a book.
 */
public final class BookDescription implements HasId, Serializable {
    public enum TOPIC {
        IDENTIFICATION, BASIC_INFORMATION, MATERIAL, QUIRES, LAYOUT, SCRIPT, DECORATION, BINDING,
        HISTORY, TEXT, AUTHOR
    }

    private static final Comparator<String> topics_comparator = new Comparator<String>() {
        @Override
        public int compare(String s1, String s2) {
            TOPIC t1 = getTopic(s1);
            TOPIC t2 = getTopic(s2);

            if (t1 == null || t2 == null) {
                return 0;
            } else {
                return t1.ordinal() - t2.ordinal();
            }
        }

        private TOPIC getTopic(String str) {
            for (TOPIC t : TOPIC.values()) {
                if (t.toString().replace("_", " ").equalsIgnoreCase(str)) {
                    return t;
                }
            }

            return null;
        }
    };
    private static final long serialVersionUID = 1L;

    /** Map of topics to text (note 'rend' to text content). */
    private Map<String, String> blocks;
    private String id;

    /** Create a new BookDescription */
    public BookDescription() {
        this.blocks = new HashMap<>();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public Map<String, String> getBlocks() {
        return blocks;
    }

    public void setBlocks(Map<String, String> blocks) {
        this.blocks = blocks;
    }

    /** Get an ordered list of topics in this description. */
    public List<String> getTopics() {
        List<String> topics = new ArrayList<>(blocks.keySet());
        Collections.sort(topics, topics_comparator);

        return topics;
    }

    public String getDescription(String topic) {
        return blocks.get(topic);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookDescription)) return false;

        BookDescription that = (BookDescription) o;

        if (blocks != null ? !blocks.equals(that.blocks) : that.blocks != null) return false;
        return !(id != null ? !id.equals(that.id) : that.id != null);

    }

    @Override
    public int hashCode() {
        int result = blocks != null ? blocks.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "BookDescription{" +
                "blocks=" + blocks +
                ", id='" + id + '\'' +
                '}';
    }
}
