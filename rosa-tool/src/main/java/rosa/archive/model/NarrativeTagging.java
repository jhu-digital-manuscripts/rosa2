package rosa.archive.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Narrative tagging data for a book, mapping narrative scenes to page ranges.
 */
public class NarrativeTagging implements HasId, Iterable<BookScene> {

    private String id;
    private List<BookScene> scenes;

    /**
     * Creates an empty NarrativeTagging.
     */
    public NarrativeTagging() {
        this.scenes = new ArrayList<>();
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
     * Returns the list of scenes tagged in this book.
     *
     * @return the scenes list
     */
    public List<BookScene> getScenes() {
        return scenes;
    }

    /**
     * Sets the list of scenes.
     *
     * @param scenes the scenes to set
     */
    public void setScenes(List<BookScene> scenes) {
        this.scenes = scenes;
    }

    @Override
    public Iterator<BookScene> iterator() {
        return scenes.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NarrativeTagging that)) return false;
        return Objects.equals(id, that.id) &&
                Objects.equals(scenes, that.scenes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, scenes);
    }

    @Override
    public String toString() {
        return "NarrativeTagging{" +
                "id='" + id + '\'' +
                ", scenes=" + scenes +
                '}';
    }
}
