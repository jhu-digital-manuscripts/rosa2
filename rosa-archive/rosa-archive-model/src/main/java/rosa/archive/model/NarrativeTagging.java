package rosa.archive.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public class NarrativeTagging implements HasId, IsSerializable, Iterable<BookScene> {

    private String id;
    private List<BookScene> scenes;

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

    public List<BookScene> getScenes() {
        return scenes;
    }

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
        if (!(o instanceof NarrativeTagging)) return false;

        NarrativeTagging tagging = (NarrativeTagging) o;

        if (id != null ? !id.equals(tagging.id) : tagging.id != null) return false;
        if (scenes != null ? !scenes.equals(tagging.scenes) : tagging.scenes != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (scenes != null ? scenes.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "NarrativeTagging{" +
                "id='" + id + '\'' +
                ", scenes=" + scenes +
                '}';
    }
}
