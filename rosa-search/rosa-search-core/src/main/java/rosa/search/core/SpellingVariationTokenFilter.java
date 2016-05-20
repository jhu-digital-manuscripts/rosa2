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

    private boolean equalsIgnoreCase(char[] a, char[] a2) {
        if (a==a2)
            return true;
        if (a==null || a2==null)
            return false;

        int length = a.length;
        if (a2.length != length)
            return false;

        for (int i=0; i<length; i++)
            if (!equalsIgnoreCase(a[i], a2[i]))
                return false;

        return true;
    }

    private boolean equalsIgnoreCase(char a, char b) {
        return Character.toLowerCase(a) == Character.toLowerCase(b);
    }

    private void doReplace(String replacement, String variant) {
        char[] in_buff = termAtt.buffer();
        char[] var_buff = variant.toCharArray();

        int in_len = termAtt.length();
        int var_len = variant.length();

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
        if (termAtt.toString().equals("null") || termAtt.toString().equalsIgnoreCase(replacement)) {
            return;
        }

        for (int i = 0; i < in_len - var_len + 1; i++) {
            char[] in_frag = Arrays.copyOfRange(in_buff, i, i + var_len);

            // If this fragment matches the variant, replace it with 'replacement'
            if (equalsIgnoreCase(in_frag, var_buff)) {
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
        if (!termAtt.toString().equals(replacement) && termAtt.toString().contains(variant)) {
            String in_buff = new String(termAtt.buffer(), 0, termAtt.length());
            termAtt.setEmpty();
            termAtt.append(in_buff.trim().toLowerCase().replace(variant.toLowerCase(), replacement));
        }
    }
}
