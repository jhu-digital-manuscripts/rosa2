package rosa.search.core.lucene;

import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.pattern.PatternTokenizer;

/**
 * Tokenize image names so they can be searched without leading zeroes.
 */
public class ImageNameAnalyzer extends Analyzer {
    private final Pattern pattern = Pattern.compile("\\s+|^0*|\\.0*");
    
    @Override
    protected TokenStreamComponents createComponents(String arg0) {
        Tokenizer tokenizer = new PatternTokenizer(pattern, -1);
        TokenStream filter = new LowerCaseFilter(tokenizer);

        return new TokenStreamComponents(tokenizer, filter);
    }
}
