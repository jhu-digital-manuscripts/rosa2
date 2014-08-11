package rosa.archive.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public class NarrativeTagging implements IsSerializable, Iterable<BookScene> {

    private List<BookScene> scenes;

    public NarrativeTagging() {
        this.scenes = new ArrayList<>();
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

        NarrativeTagging that = (NarrativeTagging) o;

        if (scenes != null ? !scenes.equals(that.scenes) : that.scenes != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return scenes != null ? scenes.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "NarrativeTagging{" +
                "scenes=" + scenes +
                '}';
    }
}
