package rosa.archive.aor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Vocab {

    // Language code -> vocab word frequency
    final Map<String, Map<String, Integer>> map;

    public Vocab() {
        map = new HashMap<>();
    }

    public void update(Vocab vocab) {
        for (Entry<String, Map<String, Integer>> entry : vocab.map.entrySet()) {
            update(entry.getKey(), entry.getValue());
        }
    }

    public void update(String language, Map<String, Integer> map) {
        if (this.map.containsKey(language)) {
            AoRVocabUtil.updateVocab(this.map.get(language), map);
        } else {
            this.map.put(language, map);
        }
    }

    public void update(String language, List<String> words) {
        if (map.containsKey(language)) {
            AoRVocabUtil.updateVocab(map.get(language), words);
        } else {
            Map<String, Integer> vocabMap = new HashMap<>();
            AoRVocabUtil.updateVocab(vocabMap, words);

            map.put(language, vocabMap);
        }
    }

    public Map<String, Integer> getVocab(String language) {
        return map.get(language);
    }

    public Set<String> getLanguages() {
        return map.keySet();
    }
}
