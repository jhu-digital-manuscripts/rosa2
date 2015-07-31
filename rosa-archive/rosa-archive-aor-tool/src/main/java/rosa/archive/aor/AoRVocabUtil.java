package rosa.archive.aor;

import java.util.Collection;
import java.util.Map;

public class AoRVocabUtil {

    public static void updateVocab(Map<String, Integer> vocab, String word, int count) {
        if (vocab.containsKey(word)) {
            vocab.put(word, vocab.get(word) + count);
        } else {
            vocab.put(word, 1);
        }
    }

    public static void updateVocab(Map<String, Integer> vocab, Collection<String> words) {
        for (String word: words) {
            updateVocab(vocab, word, 1);
        }
    }

    public static void updateVocab(Map<String, Integer> vocab1, Map<String, Integer> vocab2) {
        for (String word: vocab2.keySet()) {
            updateVocab(vocab1, word, vocab2.get(word));
        }
    }

    // Turn text into words and punctuation
    public static String[] parse_text(String text) {
        text = text.trim().replaceAll("\\p{Punct}", " ");

        return text.trim().split("\\s+");
    }

    public static int count_words(String text) {
        return parse_text(text).length;
    }
}
