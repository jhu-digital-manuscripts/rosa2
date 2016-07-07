package rosa.search.core.analyzer;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardFilter;
import rosa.search.core.CharacterRemoverTokenFilter;
import rosa.search.core.RosaStandardTokenizer;

public class RosaMarkedSpanishAnalyzer extends RosaSpanishAnalyzer {
    private final char[] toRemove;

    public RosaMarkedSpanishAnalyzer(char... toRemove) {
        this.toRemove = toRemove != null ? toRemove : new char[0];
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        final Tokenizer source = new RosaStandardTokenizer(toRemove);
        TokenStream result = buildResultTokenStream(new StandardFilter(source));

        return new TokenStreamComponents(source, result);
    }

    protected TokenStream buildResultTokenStream(TokenStream result) {
        if (toRemove.length > 0) {
            result = new CharacterRemoverTokenFilter(result, toRemove);
        }
        return super.buildResultTokenStream(result);
    }
}
