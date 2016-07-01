package rosa.search.core.analyzer;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.util.CharTokenizer;
import rosa.search.core.CharacterRemoverTokenFilter;
import rosa.search.core.RosaStandardTokenizer;

public class RosaMarkedEnglishAnalyzer extends RosaEnglishAnalyzer {
    private final Character[] charsToRemove;

    public RosaMarkedEnglishAnalyzer(char ... toRemove) {
        if (toRemove != null) {
            charsToRemove = new Character[toRemove.length];
            for (int i = 0; i < toRemove.length; i++) {
                charsToRemove[i] = toRemove[i];
            }
        } else {
            charsToRemove = null;
        }
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer source = new RosaStandardTokenizer();
        final TokenStream result = buildResultTokenStream(new StandardFilter(source));

        return new TokenStreamComponents(source, result);
    }

    @Override
    protected TokenStream buildResultTokenStream(TokenStream result) {
        // Can chain more token filters here
        if (charsToRemove != null && charsToRemove.length > 0) {
            result = new CharacterRemoverTokenFilter(result, charsToRemove);
        }
        result = super.buildResultTokenStream(result);
        return result;
    }

    public static class TranscriberMarkTokenizer extends CharTokenizer {

        @Override
        protected boolean isTokenChar(int c) {
            return c != '[' && c != ']';
        }
    }

}
