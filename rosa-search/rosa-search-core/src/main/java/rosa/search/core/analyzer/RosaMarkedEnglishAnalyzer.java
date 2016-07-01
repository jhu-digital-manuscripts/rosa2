package rosa.search.core.analyzer;

import org.apache.lucene.analysis.TokenStream;
import rosa.search.core.CharacterRemoverTokenFilter;

public class RosaMarkedEnglishAnalyzer extends RosaEnglishAnalyzer {

    @Override
    protected TokenStream buildResultTokenStream(TokenStream result) {
        // Can chain more token filters here
        result = new CharacterRemoverTokenFilter(result, 'a', 'b');
        result = super.buildResultTokenStream(result);
        return result;
    }

}
