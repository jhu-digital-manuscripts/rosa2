package rosa.archive.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class NarrativeSections implements IsSerializable {

    private ArrayList<NarrativeScene> scenes;

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

    public int findIndexOfSceneById(String id) {
        // TODO stub
        return -1;
    }

    public void setScenes(ArrayList<NarrativeScene> scenes) {
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
}
