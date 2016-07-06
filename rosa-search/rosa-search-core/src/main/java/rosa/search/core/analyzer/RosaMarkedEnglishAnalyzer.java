package rosa.search.core.analyzer;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardFilter;
import rosa.search.core.CharacterRemoverTokenFilter;
import rosa.search.core.RosaStandardTokenizer;

/**
 * Extension of {@link RosaEnglishAnalyzer} that is able to ignore a set of characters
 * during indexing. Indexed data is
 */
public class RosaMarkedEnglishAnalyzer extends RosaEnglishAnalyzer {
    private final char[] toRemove;

    public RosaMarkedEnglishAnalyzer(char ... toRemove) {
        this.toRemove = toRemove != null ? toRemove : new char[0];
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer source = new RosaStandardTokenizer(toRemove);
        final TokenStream result = buildResultTokenStream(new StandardFilter(source));

        return new TokenStreamComponents(source, result);
    }

    @Override
    protected TokenStream buildResultTokenStream(TokenStream result) {
        // Can chain more token filters here
        if (toRemove.length > 0) {
            result = new CharacterRemoverTokenFilter(result, toRemove);
        }
        result = super.buildResultTokenStream(result);
        return result;
    }

}
