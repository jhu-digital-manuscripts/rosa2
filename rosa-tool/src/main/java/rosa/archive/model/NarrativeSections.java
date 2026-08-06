package rosa.archive.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Collection of narrative scenes present in a book collection.
 * Scenes may or may not be present in individual books in the collection,
 * and are useful for mapping scenes onto specific books.
 */
public class NarrativeSections implements HasId {

    private String id;
    private List<NarrativeScene> scenes;

    /**
     * Creates an empty NarrativeSections collection.
     */
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
     * Returns the list of all scenes.
     *
     * @return the list of scenes
     */
    public List<NarrativeScene> asScenes() {
        return scenes;
    }

    /**
     * Returns the number of scenes.
     *
     * @return the scene count
     */
    public int numberOfScenes() {
        return scenes.size();
    }

    /**
     * Finds the index of a scene by its id (whitespace-insensitive, case-insensitive).
     *
     * @param id the scene id to find
     * @return the index of the scene, or -1 if not found
     */
    public int findIndexOfSceneById(String id) {
        String normalizedId = id.replaceAll("\\s+", "");
        for (int i = 0; i < scenes.size(); i++) {
            String sceneId = scenes.get(i).getId().replaceAll("\\s+", "");
            if (sceneId.equalsIgnoreCase(normalizedId)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Sets the list of scenes.
     *
     * @param scenes the scenes to set
     */
    public void setScenes(List<NarrativeScene> scenes) {
        this.scenes = scenes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NarrativeSections that)) return false;
        return Objects.equals(id, that.id) &&
                Objects.equals(scenes, that.scenes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, scenes);
    }

    @Override
    public String toString() {
        return "NarrativeSections{" +
                "id='" + id + '\'' +
                ", scenes=" + scenes +
                '}';
    }
}
