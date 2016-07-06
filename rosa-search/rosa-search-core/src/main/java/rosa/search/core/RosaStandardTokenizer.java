package rosa.search.core;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizerImpl;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.AttributeFactory;

import java.io.IOException;
import java.util.Arrays;

public class RosaStandardTokenizer extends Tokenizer {
    /** A private instance of the JFlex-constructed scanner */
    private StandardTokenizerImpl scanner;

    // TODO: how can we remove these old types?!
    public static final int ALPHANUM          = 0;
    /** @deprecated (3.1) */
    @Deprecated
    public static final int APOSTROPHE        = 1;
    /** @deprecated (3.1) */
    @Deprecated
    public static final int ACRONYM           = 2;
    /** @deprecated (3.1) */
    @Deprecated
    public static final int COMPANY           = 3;
    public static final int EMAIL             = 4;
    /** @deprecated (3.1) */
    @Deprecated
    public static final int HOST              = 5;
    public static final int NUM               = 6;
    /** @deprecated (3.1) */
    @Deprecated
    public static final int CJ                = 7;

    /** @deprecated (3.1) */
    @Deprecated
    public static final int ACRONYM_DEP       = 8;

    public static final int SOUTHEAST_ASIAN = 9;
    public static final int IDEOGRAPHIC = 10;
    public static final int HIRAGANA = 11;
    public static final int KATAKANA = 12;
    public static final int HANGUL = 13;

    /** String token types that correspond to token type int constants */
    public static final String [] TOKEN_TYPES = new String [] {
            "<ALPHANUM>",
            "<APOSTROPHE>",
            "<ACRONYM>",
            "<COMPANY>",
            "<EMAIL>",
            "<HOST>",
            "<NUM>",
            "<CJ>",
            "<ACRONYM_DEP>",
            "<SOUTHEAST_ASIAN>",
            "<IDEOGRAPHIC>",
            "<HIRAGANA>",
            "<KATAKANA>",
            "<HANGUL>"
    };

    private int skippedPositions;

    private int maxTokenLength = StandardAnalyzer.DEFAULT_MAX_TOKEN_LENGTH;

    /** Set the max allowed token length.  Any token longer
     *  than this is skipped. */
    public void setMaxTokenLength(int length) {
        if (length < 1) {
            throw new IllegalArgumentException("maxTokenLength must be greater than zero");
        }
        this.maxTokenLength = length;
        scanner.setBufferSize(Math.min(length, 1024 * 1024)); // limit buffer size to 1M chars
    }

    /** @see #setMaxTokenLength */
    public int getMaxTokenLength() {
        return maxTokenLength;
    }

    /**
     * Creates a new instance of the {@link org.apache.lucene.analysis.standard.StandardTokenizer}.  Attaches
     * the <code>input</code> to the newly created JFlex scanner.

     * See http://issues.apache.org/jira/browse/LUCENE-1068
     */
    public RosaStandardTokenizer() {
        init();
    }

    /**
     * Creates a new StandardTokenizer with a given {@link org.apache.lucene.util.AttributeFactory}
     */
    public RosaStandardTokenizer(AttributeFactory factory) {
        super(factory);
        init();
    }

    private void init() {
        this.scanner = new StandardTokenizerImpl(input);
    }

    // this tokenizer generates three attributes:
    // term offset, positionIncrement and type
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
    private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
    private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);


    StringBuilder sb = new StringBuilder();

    /*
     * (non-Javadoc)
     *
     * @see org.apache.lucene.analysis.TokenStream#next()
     */
    @Override
    public final boolean incrementToken() throws IOException {
        clearAttributes();
        skippedPositions = 0;

        boolean inWordWithMark = false;

        while(true) {
            int tokenType = scanner.getNextToken();

            if (tokenType == StandardTokenizerImpl.YYEOF) {
                return false;
            }

            if (scanner.yylength() <= maxTokenLength) {
                posIncrAtt.setPositionIncrement(skippedPositions+1);
                scanner.getText(termAtt);
                final int start = scanner.yychar();
                offsetAtt.setOffset(correctOffset(start), correctOffset(start+termAtt.length()));
                typeAtt.setType(StandardTokenizer.TOKEN_TYPES[tokenType]);

                char delimiter = scanner.yycharat(scanner.yylength());
                if (delimiter == '[' || delimiter == ']') {
                    inWordWithMark = true;
                    sb.append(termAtt.buffer(), 0, termAtt.length());
                    continue;
                } else if (inWordWithMark) {
                    sb.append(termAtt.buffer(), 0, termAtt.length());
                }
                if (sb.length() > 0) {
                    termAtt.setEmpty().append(sb);
                    sb = new StringBuilder();
                }

                return true;
            } else
                // When we skip a too-long term, we still increment the
                // position increment
                skippedPositions++;
        }
    }

    @Override
    public final void end() throws IOException {
        super.end();
        // set final offset
        int finalOffset = correctOffset(scanner.yychar() + scanner.yylength());
        offsetAtt.setOffset(finalOffset, finalOffset);
        // adjust any skipped tokens
        posIncrAtt.setPositionIncrement(posIncrAtt.getPositionIncrement()+skippedPositions);
    }

    @Override
    public void close() throws IOException {
        super.close();
        scanner.yyreset(input);
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        scanner.yyreset(input);
        skippedPositions = 0;
    }
}
