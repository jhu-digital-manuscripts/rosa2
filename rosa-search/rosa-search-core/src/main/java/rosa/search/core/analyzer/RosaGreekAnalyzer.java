package rosa.search.core.analyzer;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.el.GreekAnalyzer;
import org.apache.lucene.analysis.el.GreekLowerCaseFilter;
import org.apache.lucene.analysis.el.GreekStemFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.standard.std40.StandardTokenizer40;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.io.Reader;

public class RosaGreekAnalyzer extends StopwordAnalyzerBase {
    /** File containing default Greek stopwords. */
    public final static String DEFAULT_STOPWORD_FILE = "stopwords.txt";

    /**
     * Returns a set of default Greek-stopwords
     * @return a set of default Greek-stopwords
     */
    public static final CharArraySet getDefaultStopSet(){
        return DefaultSetHolder.DEFAULT_SET;
    }

    private static class DefaultSetHolder {
        private static final CharArraySet DEFAULT_SET;

        static {
            try {
                DEFAULT_SET = loadStopwordSet(false, GreekAnalyzer.class, DEFAULT_STOPWORD_FILE, "#");
            } catch (IOException ex) {
                // default set should always be present as it is part of the
                // distribution (JAR)
                throw new RuntimeException("Unable to load default stopword set");
            }
        }
    }

    /**
     * Builds an analyzer with the default stop words.
     */
    public RosaGreekAnalyzer() {
        this(DefaultSetHolder.DEFAULT_SET);
    }

    /**
     * Builds an analyzer with the given stop words.
     * <p>
     * <b>NOTE:</b> The stopwords set should be pre-processed with the logic of
     * {@link GreekLowerCaseFilter} for best results.
     *
     * @param stopwords a stopword set
     */
    public RosaGreekAnalyzer(CharArraySet stopwords) {
        super(stopwords);
    }

    /**
     * Creates
     * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
     * used to tokenize all the text in the provided {@link Reader}.
     *
     * @return {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
     *         built from a {@link StandardTokenizer} filtered with
     *         {@link GreekLowerCaseFilter}, {@link StandardFilter},
     *         {@link StopFilter}, and {@link GreekStemFilter}
     */
    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        final Tokenizer source = new StandardTokenizer();
        TokenStream result = buildResultTokenStream(new GreekLowerCaseFilter(source));

        return new TokenStreamComponents(source, result);
    }

    protected TokenStream buildResultTokenStream(TokenStream result) {
        result = new StandardFilter(result);
        result = new StopFilter(result, stopwords);
        return new GreekStemFilter(result);
    }
}
