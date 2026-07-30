package rosa.archive.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Illustration titles associated with a collection. Maps title identifiers
 * to their display text.
 */
public class IllustrationTitles implements HasId {

    private String id;
    private Map<String, String> data;

    /**
     * Creates an empty IllustrationTitles instance.
     */
    public IllustrationTitles() {
        this.data = new HashMap<>();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Sets the title data map.
     *
     * @param data the map of title id to title text
     */
    public void setData(Map<String, String> data) {
        this.data = data;
    }

    /**
     * Checks whether a title exists for the given id.
     *
     * @param id the title id
     * @return {@code true} if a title exists
     */
    public boolean hasTitle(String id) {
        return data.containsKey(id);
    }

    /**
     * Returns the title text for the given id.
     *
     * @param id the title id
     * @return the title text, or {@code null} if not found
     */
    public String getTitleById(String id) {
        return data.get(id);
    }

    /**
     * Returns all title identifiers.
     *
     * @return set of all title ids
     */
    public Set<String> getAllIds() {
        return data.keySet();
    }

    /**
     * Finds the id of the given title text.
     *
     * @param title the title text to search for
     * @return the id of the title, or {@code null} if not found
     */
    public String findIdOfTitle(String title) {
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (entry.getValue().equals(title)) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IllustrationTitles that)) return false;
        return Objects.equals(id, that.id) &&
                Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, data);
    }

    @Override
    public String toString() {
        return "IllustrationTitles{" +
                "id='" + id + '\'' +
                ", data=" + data +
                '}';
    }
}
