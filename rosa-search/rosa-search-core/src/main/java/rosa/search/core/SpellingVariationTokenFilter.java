package rosa.search.core;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Token filter used to normalize spelling variation.
 */
public class SpellingVariationTokenFilter extends TokenFilter {
    private final Map<String, Set<String>> spellingEquivalence;
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    public SpellingVariationTokenFilter(TokenStream input) {
        this(input, new HashMap<>());
    }

    public SpellingVariationTokenFilter(TokenStream input, Map<String, Set<String>> spellingEquivalence) {
        super(input);
        this.spellingEquivalence = spellingEquivalence;
    }

    public void addSpellingVariant(String key, String ... variants) {
        if (spellingEquivalence == null || variants == null || variants.length == 0) {
            return;
        }
        spellingEquivalence.put(key, new HashSet<>(Arrays.asList(variants)));
    }


    @Override
    public boolean incrementToken() throws IOException {
        if (input.incrementToken()) {
            String buffer = new String(termAtt.buffer());
            int len = termAtt.length();



            return true;
        } else {
            return false;
        }
    }
}
