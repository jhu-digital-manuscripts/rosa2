package rosa.archive.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.ArrayList;
import java.util.List;

/**
 * Collection of scenes present in a collection. Scenes may or may not be present in individual
 * books in the collection. Useful for mapping scenes onto the books.
 */
public class NarrativeSections implements HasId, IsSerializable {

    private String id;
    private List<NarrativeScene> scenes;

    public NarrativeSections() {
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
     * Get a list of all scenes
     *
     * @return
     *          list of scenes
     */
    public List<NarrativeScene> asScenes() {
        return scenes;
    }

    /**
     * @return
     *          the number of scenes
     */
    public int numberOfScenes() {
        return scenes.size();
    }

    /**
     * @param id
     *          id of the scene to find
     * @return
     *          index of the scene in question if present. If the scene is not present, -1 is returned.
     */
    public int findIndexOfSceneById(String id) {
        for (int i = 0; i < scenes.size(); i++) {
            if (scenes.get(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    public void setScenes(List<NarrativeScene> scenes) {
        this.scenes = scenes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NarrativeSections)) return false;

        NarrativeSections sections = (NarrativeSections) o;

        if (id != null ? !id.equals(sections.id) : sections.id != null) return false;
        if (scenes != null ? !scenes.equals(sections.scenes) : sections.scenes != null) return false;

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
        return "NarrativeSections{" +
                "id='" + id + '\'' +
                ", scenes=" + scenes +
                '}';
    }
}
