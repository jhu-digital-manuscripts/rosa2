package rosa.search.core;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
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
        this(input, new char[0]);
    }

    public CharacterRemoverTokenFilter(TokenStream input, char... toRemove) {
        super(input);

        charsToRemove = new HashSet<>();
        if (toRemove != null) {
            for (char c : toRemove) {
                charsToRemove.add(c);
            }
        }
    }

    @Override
    public final boolean incrementToken() throws IOException {
        if (input.incrementToken()) {
            doRemove(termAtt.buffer(), 0, termAtt.length());
            return true;
        } else {
            return false;
        }
    }

    private void doRemove(char[] currentBuffer, int offset, int length) {
        int count = 0;
        char[] newBuffer = new char[length];

        for (int i = offset; i < length; i++) {
            if (charsToRemove.contains(currentBuffer[i])) {
                // Skip element if char is found.
                count++;
                i++;
            }

            newBuffer[i - count] = currentBuffer[i];
        }

        termAtt.copyBuffer(newBuffer, 0, length - count);
        termAtt.resizeBuffer(length - count);
    }
}
