package rosa.search.core;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
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
            for (Entry<String, Set<String>> entry : spellingEquivalence.entrySet()) {
                String key = entry.getKey();

                for (String variant : entry.getValue()) {
                    doReplace(key, variant);
                }
            }

            return true;
        } else {
            return false;
        }
    }

    private void doReplace(String replacement, String variant) {
        char[] in_buff = termAtt.buffer();
        char[] var_buff = variant.toCharArray();

        int in_len = termAtt.length();
        int var_len = variant.length();

        for (int i = 0; i < in_len - var_len; i++) {
            char[] in_frag = Arrays.copyOfRange(in_buff, i, var_len);

            // If this fragment matches the variant, replace it with 'replacement'
            if (Arrays.equals(in_frag, var_buff)) {
                String prefix = new String(Arrays.copyOfRange(in_buff, 0, i));
                String suffix = new String(Arrays.copyOfRange(in_buff, i + var_len, in_len));

                termAtt.setEmpty();
                termAtt.append(prefix).append(replacement).append(suffix);
            }
        }
    }
}
