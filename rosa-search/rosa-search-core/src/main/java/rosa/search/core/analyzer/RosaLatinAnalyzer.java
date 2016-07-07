package rosa.search.core.analyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.standard.StandardFilter;
import rosa.lucene.la.LatinStemFilter;

public class RosaLatinAnalyzer extends Analyzer {

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer source = new WhitespaceTokenizer();
        TokenStream result = buildResultTokenStream(new StandardFilter(source));

        return new TokenStreamComponents(source, result);
    }

    protected TokenStream buildResultTokenStream(TokenStream result) {
        return new LatinStemFilter(result);
    }
}
