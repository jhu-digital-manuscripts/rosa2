package rosa.archive.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.ArrayList;
import java.util.List;

/**
 * Collection of scenes present in a collection. Scenes may or may not be present in individual
 * books in the collection. Useful for mapping scenes onto the books.
 */
public class NarrativeSections implements IsSerializable {

    private List<NarrativeScene> scenes;

    public NarrativeSections() {
        this.scenes = new ArrayList<>();
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

        NarrativeSections that = (NarrativeSections) o;

        if (!scenes.equals(that.scenes)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return scenes.hashCode();
    }

    @Override
    public String toString() {
        return "NarrativeSections{" +
                "scenes=" + scenes +
                '}';
    }
}
