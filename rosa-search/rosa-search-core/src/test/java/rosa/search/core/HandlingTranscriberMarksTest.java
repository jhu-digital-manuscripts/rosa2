package rosa.search.core;

import org.apache.lucene.document.Document;
import org.junit.Before;
import org.junit.Test;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.search.core.analyzer.MarkedOldFrenchAnalyzer;
import rosa.search.core.analyzer.RosaLanguageAnalyzers;
import rosa.search.core.analyzer.RosaMarkedEnglishAnalyzer;
import rosa.search.core.analyzer.RosaMarkedFrenchAnalyzer;
import rosa.search.core.analyzer.RosaMarkedGreekAnalyzer;
import rosa.search.core.analyzer.RosaMarkedItalianAnalyzer;
import rosa.search.core.analyzer.RosaMarkedLatinAnalyzer;
import rosa.search.core.analyzer.RosaMarkedSpanishAnalyzer;
import rosa.search.model.Query;
import rosa.search.model.SearchField;
import rosa.search.model.SearchFieldType;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HandlingTranscriberMarksTest {
    private static final char[] EXCLUDE_LIST = { '[', ']', 'w' };
    private BaseLuceneMapper mapper;

    private enum SearchFields implements SearchField {
        ID(false, false, SearchFieldType.STRING),
        TEXT(true, true, SearchFieldType.ENGLISH, SearchFieldType.FRENCH, SearchFieldType.LATIN),
        EN_TEXT(true, true, SearchFieldType.ENGLISH),
        FR_TEXT(true, true, SearchFieldType.FRENCH),
        FR_OL_TEXT(true, true, SearchFieldType.OLD_FRENCH),
        REMOVAL(true, true, SearchFieldType.ENGLISH);

        private final SearchFieldType[] types;
        private final boolean context;
        private final boolean include;

        SearchFields(boolean context, boolean include, SearchFieldType... types) {
            this.types = types;
            this.context = context;
            this.include = include;
        }

        @Override
        public boolean isContext() {
            return context;
        }

        @Override
        public boolean includeValue() {
            return include;
        }

        @Override
        public SearchFieldType[] getFieldTypes() {
            return types;
        }

        @Override
        public String getFieldName() {
            return name();
        }
    }

    @Before
    public void setup() {
        final RosaLanguageAnalyzers languageAnalyzers = new RosaLanguageAnalyzers.Builder()
                .englishAnalyzer(new RosaMarkedEnglishAnalyzer(EXCLUDE_LIST))
                .frenchAnalyzer(new RosaMarkedFrenchAnalyzer(EXCLUDE_LIST))
                .oldFrenchAnalyzer(new MarkedOldFrenchAnalyzer(EXCLUDE_LIST))
                .greekAnalyzer(new RosaMarkedGreekAnalyzer(EXCLUDE_LIST))
                .italianAnalyzer(new RosaMarkedItalianAnalyzer(EXCLUDE_LIST))
                .spanishAnalyzer(new RosaMarkedSpanishAnalyzer(EXCLUDE_LIST))
                .latinAnalyzer(new RosaMarkedLatinAnalyzer(EXCLUDE_LIST))
                .build();

        mapper = new BaseLuceneMapper(languageAnalyzers, SearchFields.values()) {
            @Override
            public SearchField getIdentifierSearchField() {
                return SearchFields.ID;
            }

            @Override
            public List<Document> createDocuments(BookCollection col, Book book) throws IOException {
                throw new UnsupportedOperationException();
            }
        };
    }
    
    @Test
    public void testCreateLuceneQuerySimple() {
        org.apache.lucene.search.Query result = mapper.createLuceneQuery(new Query(SearchFields.ID, "bessie"));

        assertNotNull(result);

        String lucene_query = result.toString();

        assertEquals("ID.STRING:bessie", lucene_query);
    }

    @Test
    public void testCreateLuceneQuerySimplePhrase() {
        org.apache.lucene.search.Query result = mapper.createLuceneQuery(new Query(SearchFields.TEXT, "bessie \"good cow\""));

        assertNotNull(result);

        String lucene_query = result.toString();

        assertTrue(lucene_query.contains("TEXT.ENGLISH:\"good co\""));
        assertTrue(lucene_query.contains("TEXT.FRENCH:\"good co\""));
    }

    @Test
    public void testCreateLuceneQueryFromAnyString() {
        org.apache.lucene.search.Query result = mapper.createLuceneQuery("bessie \"good cow\"");

        assertNotNull(result);

        String lucene_query = result.toString();

        assertTrue(lucene_query.contains("TEXT.ENGLISH:\"good co\""));
        assertTrue(lucene_query.contains("TEXT.FRENCH:\"good co\""));
    }

    @Test
    public void testOldFrenchSpelling() {
        checkSpellings(
                "bessie, bessje, and bessye are the same cow.",
                SearchFields.FR_OL_TEXT,
                new String[] {"OLD_FRENCH:besi"},
                new String[] {"OLD_FRENCH:besy", "OLD_FRENCH:besj"},
                false
        );
        checkSpellings(
                "bessie, be[ss]je, and [bess]ye are the same cow.",
                SearchFields.FR_OL_TEXT,
                new String[] {"OLD_FRENCH:besi"},
                new String[] {"OLD_FRENCH:besy", "OLD_FRENCH:besj", "cow"},
                false
        );
    }

    @Test
    public void testEnglishSpelling() {
        checkSpellings(
                "bessie, bessje, and bessye are the same cow.",
                SearchFields.EN_TEXT,
                new String[] {"bessi", "bessy", "bessj"},
                null,
                false
        );
        checkSpellings(
                "bessie, be[ss]je, and [bess]ye are the same cow.",
                SearchFields.EN_TEXT,
                new String[] {"bessi", "bessy", "bessj"},
                null,
                false
        );
    }

    @Test
    public void testFrenchSpelling() {
        checkSpellings(
                "bessie, bessje, and bessye are the same cow.",
                SearchFields.FR_TEXT,
                new String[] {"FR_TEXT.FRENCH:bes", "FR_TEXT.FRENCH:besj", "FR_TEXT.FRENCH:besy"},
                null,
                false
        );

        checkSpellings(
                "bessie, be[ss]je, and [bess]ye are the same cow.",
                SearchFields.FR_TEXT,
                new String[] {"FR_TEXT.FRENCH:bes", "FR_TEXT.FRENCH:besj", "FR_TEXT.FRENCH:besy"},
                null,
                false
        );
    }

    private void checkSpellings(String original, SearchFields field, String[] shouldInclude, String[] shouldNotInclude,
                                boolean debug) {
        org.apache.lucene.search.Query result = mapper.createLuceneQuery(new Query(field, original));
        String lucene_query = result.toString();

        if (debug) {
            System.out.println("ORIGINAL  QUERY :: " + original);
            System.out.println("GENERATED QUERY :: " + lucene_query);
        }

        assertNotNull("Could not generate query, result was NULL.", lucene_query);

        if (shouldInclude != null) {
            for (String str : shouldInclude) {
                assertTrue("String not found in query that should be present. [" + str + "]", lucene_query.contains(str));
            }
        }
        if (shouldNotInclude != null) {
            for (String str : shouldNotInclude) {
                assertFalse("String found in query that should not be present. [" + str + "]", lucene_query.contains(str));
            }
        }
    }
}
