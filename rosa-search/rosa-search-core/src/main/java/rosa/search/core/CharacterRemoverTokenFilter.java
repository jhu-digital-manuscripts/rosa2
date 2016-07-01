package rosa.search.core;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Filter used to remove certain characters in a token. For example, this can be used to strip
 * straight brackets from tokens.
 */
public class CharacterRemoverTokenFilter extends TokenFilter {

    private final Set<Character> charsToRemove;
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    /**
     * Construct a token stream filtering the given input.
     *
     * @param input
     */
    public CharacterRemoverTokenFilter(TokenStream input) {
        this(input, new Character[0]);
    }

    public CharacterRemoverTokenFilter(TokenStream input, Character ... toRemove) {
        super(input);

        if (toRemove != null) {
            charsToRemove = new HashSet<>(Arrays.asList(toRemove));
        } else {
            charsToRemove = new HashSet<>();
        }
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (input.incrementToken()) {
            char[] buffer = termAtt.buffer();
            charsToRemove.stream().forEach(character -> doRemove(character, buffer));
            return true;
        } else {
            return false;
        }
    }

    private void doRemove(char toRemove, char[] currentBuffer) {
        int len = termAtt.length();
        int count = 0;
        char[] newBuffer = new char[len];

        for (int i = 0; i < len; i++) {
            if (currentBuffer[i] == toRemove) {
                // Skip element if char is found.
                count++;
                i++;
            }

            newBuffer[i - count] = currentBuffer[i];
        }

        termAtt.copyBuffer(newBuffer, 0, len - count);
        termAtt.resizeBuffer(len - count);
    }
}
