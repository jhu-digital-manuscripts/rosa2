package rosa.archive.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Illustration titles associated with a collection, not necessarily with a single book in
 * the collection.
 */
public class IllustrationTitles implements HasId, Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private Map<String, String> data;

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

    public void setData(Map<String, String> data) {
        this.data = data;
    }

    public boolean hasTitle(String id) {
        return getAllIds().contains(id);
    }

    public String getTitleById(String id) {
        return data.get(id);
    }

    public Set<String> getAllIds() {
        return data.keySet();
    }

    public String findIdOfTitle(String title) {
        for (String id : data.keySet()) {
            if (data.get(id).equals(title)) {
                return id;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IllustrationTitles)) return false;

        IllustrationTitles titles = (IllustrationTitles) o;

        if (data != null ? !data.equals(titles.data) : titles.data != null) return false;
        if (id != null ? !id.equals(titles.id) : titles.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (data != null ? data.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "IllustrationTitles{" +
                "id='" + id + '\'' +
                ", data=" + data +
                '}';
    }
}
