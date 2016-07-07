package rosa.search.core.analyzer;

import org.apache.lucene.analysis.TokenStream;
import rosa.lucene.la.LatinStemFilter;
import rosa.search.core.CharacterRemoverTokenFilter;
import rosa.search.core.analyzer.RosaLatinAnalyzer;

public class RosaMarkedLatinAnalyzer extends RosaLatinAnalyzer {
    private final char[] toRemove;

    public RosaMarkedLatinAnalyzer(char... toRemove) {
        this.toRemove = toRemove != null ? toRemove : new char[0];
    }

    @Override
    protected TokenStream buildResultTokenStream(TokenStream result) {
        if (toRemove.length > 0) {
            result = new CharacterRemoverTokenFilter(result, toRemove);
        }
        return new LatinStemFilter(result);
    }
}
