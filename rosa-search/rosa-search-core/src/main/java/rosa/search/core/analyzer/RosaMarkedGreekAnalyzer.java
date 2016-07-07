package rosa.search.core.analyzer;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.el.GreekLowerCaseFilter;
import org.apache.lucene.analysis.el.GreekStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import rosa.search.core.CharacterRemoverTokenFilter;

public class RosaMarkedGreekAnalyzer extends RosaGreekAnalyzer {
    private final char[] toRemove;

    public RosaMarkedGreekAnalyzer(char... toRemove) {
        this.toRemove = toRemove != null ? toRemove : new char[0];
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        final Tokenizer source = new StandardTokenizer();
        TokenStream result = buildResultTokenStream(new GreekLowerCaseFilter(source));

        return new TokenStreamComponents(source, result);
    }

    protected TokenStream buildResultTokenStream(TokenStream result) {
        if (toRemove.length > 0) {
            result = new CharacterRemoverTokenFilter(result, toRemove);
        }
        return super.buildResultTokenStream(result);
    }
}
