package rosa.search.core;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Token filter used to normalize spelling variation.
 */
public class SpellingVariationTokenFilter extends TokenFilter {
    /**
     * Map containing character or string equivalencies that should be normalized.
     *  Key: (String) reference
     *  Value: (Set&lt;String&gt;) set of variants that are equivalent and will be replaced by
     *          the reference/key.
     */
    private final Map<String, Set<String>> equivalenceTable;
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    public SpellingVariationTokenFilter(TokenStream input) {
        this(input, new HashMap<>());
    }

    public SpellingVariationTokenFilter(TokenStream input, Map<String, Set<String>> equivalenceTable) {
        super(input);
        this.equivalenceTable = equivalenceTable;
    }

    @Override
    public final boolean incrementToken() throws IOException {
        if (input.incrementToken()) {
            for (Entry<String, Set<String>> entry : equivalenceTable.entrySet()) {
                String key = entry.getKey();

                entry.getValue().stream()
                        .filter(var -> var != null && var.length() > 0)
                        .forEach(variant -> doReplace(key, variant));
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

        for (int i = 0; i < in_len; i++) {
            in_buff[i] = Character.toLowerCase(in_buff[i]);
        }
        for (int i = 0; i < var_len; i++) {
            var_buff[i] = Character.toLowerCase(var_buff[i]);
        }

        /*
         * Do no replacements if the current token is equal to the replacement term.
         * Shortcuts the case where a name variant appears within its reference
         * replacement.
         *
         * EX:
         * Reference: L'Amans
         * Variant:   Amans
         *
         * Without this check, the token "L'Amans" would be replaced with "L'L'Amans"
         */
        if (termAtt.toString().equals("null") || termAtt.toString().equals(replacement.toLowerCase())) {
            return;
        }

        for (int i = 0; i < in_len - var_len + 1; i++) {
            // Skip if current char does not match first char of variant
            if (in_buff[i] != var_buff[0]) {
                continue;
            }

            char[] in_frag = Arrays.copyOfRange(in_buff, i, i + var_len);

            // If this fragment matches the variant, replace it with 'replacement'
            if (Arrays.equals(in_frag, var_buff)) {
                String prefix = new String(Arrays.copyOfRange(in_buff, 0, i));
                String suffix = new String(Arrays.copyOfRange(in_buff, i + var_len, in_len));
                i += var_len - 1;

                termAtt.setEmpty();
                termAtt.append(prefix).append(replacement).append(suffix);
            }
        }
    }

    /**
     * Simple replacement using Strings. Slower than using char arrays.
     *
     * @param replacement replacement string
     * @param variant variant string to be replaced
     */
    private void doReplace2(String replacement, String variant) {
        String in_buff = new String(termAtt.buffer(), 0, termAtt.length());
        termAtt.setEmpty();
        termAtt.append(in_buff.trim().replace(variant, replacement));
    }
}
