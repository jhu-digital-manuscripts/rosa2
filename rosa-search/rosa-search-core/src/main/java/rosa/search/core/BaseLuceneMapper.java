package rosa.search.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.el.GreekAnalyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.pattern.PatternTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.simple.SimpleQueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import rosa.search.core.analyzer.RosaOldFrenchAnalyzer;
import rosa.search.core.analyzer.RosaEnglishAnalyzer;
import rosa.search.core.analyzer.RosaLanguageAnalyzers;
import rosa.search.core.analyzer.RosaLatinAnalyzer;
import rosa.search.model.QueryOperation;
import rosa.search.model.QueryTerm;
import rosa.search.model.SearchField;
import rosa.search.model.SearchFieldType;

/**
 * Handle mapping between search fields and Lucene fields, search queries and
 * Lucene queries.
 */
public abstract class BaseLuceneMapper implements LuceneMapper {
    private final RosaLanguageAnalyzers languageAnalyzers;
    private final Analyzer imagename_analyzer;
    private final Analyzer string_analyzer;
    private final Analyzer main_analyzer;

    // Search field name -> search field
    private final Map<String, SearchField> search_field_map;
    private final List<SearchField> included_search_fields;
    private final List<SearchField> context_search_fields;

    public BaseLuceneMapper(RosaLanguageAnalyzers languageAnalyzers, SearchField... fields) {
        this.languageAnalyzers = languageAnalyzers;

        this.string_analyzer = new WhitespaceAnalyzer();

        this.imagename_analyzer = new Analyzer() { // TODO r/v
            Pattern pattern = Pattern.compile("\\s+|^0*|\\.0*");
            @Override
            protected TokenStreamComponents createComponents(String arg0) {
                Tokenizer tokenizer = new PatternTokenizer(pattern, -1);
                TokenStream filter = new LowerCaseFilter(tokenizer);

                return new TokenStreamComponents(tokenizer, filter);
            }
        };

        this.search_field_map = new HashMap<>();

        Map<String, Analyzer> analyzer_map = new HashMap<>();

        for (SearchField sf : fields) {
            search_field_map.put(sf.getFieldName(), sf);

            for (SearchFieldType type : sf.getFieldTypes()) {
                String lucene_field = getLuceneField(sf, type);
                analyzer_map.put(lucene_field, get_analyzer(type));
            }
        }

        this.main_analyzer = new PerFieldAnalyzerWrapper(string_analyzer, analyzer_map);

        this.included_search_fields = new ArrayList<>();
        this.context_search_fields = new ArrayList<>();

        for (SearchField sf : fields) {
            if (sf.includeValue()) {
                included_search_fields.add(sf);
            }

            if (sf.isContext()) {
                context_search_fields.add(sf);
            }
        }
    }

    public BaseLuceneMapper(SearchField... fields) {
        this(
                new RosaLanguageAnalyzers.Builder()
                        .englishAnalyzer(new RosaEnglishAnalyzer())
                        .frenchAnalyzer(new FrenchAnalyzer())
                        .oldFrenchAnalyzer(new RosaOldFrenchAnalyzer())
                        .greekAnalyzer(new GreekAnalyzer())
                        .italianAnalyzer(new ItalianAnalyzer())
                        .spanishAnalyzer(new SpanishAnalyzer())
                        .latinAnalyzer(new RosaLatinAnalyzer())
                        .build(),
                fields);
    }

    protected void addNameVariant(String name_id, String... variants) {
        ((RosaOldFrenchAnalyzer) languageAnalyzers.oldFrenchAnalyzer()).addNameVariant(name_id, variants);
    }

    public String getLuceneField(SearchField sf, SearchFieldType type) {
        return sf.getFieldName() + "." + type.name();
    }

    private Analyzer get_analyzer(SearchFieldType type) {
        switch (type) {
            case IMAGE_NAME:
                return imagename_analyzer;
            case STRING:
                return string_analyzer;
            case ENGLISH:
                return languageAnalyzers.englishAnalyzer();
            case FRENCH:
                return languageAnalyzers.frenchAnalyzer();
            case OLD_FRENCH:
                return languageAnalyzers.oldFrenchAnalyzer();
            case ITALIAN:
                return languageAnalyzers.italianAnalyzer();
            case GREEK:
                return languageAnalyzers.greekAnalyzer();
            case SPANISH:
                return languageAnalyzers.spanishAnalyzer();
            case LATIN:
                return languageAnalyzers.latinAnalyzer();
            default:
                return null;
        }
    }

    public Analyzer getAnalyzer() {
        return main_analyzer;
    }

    public Query createLuceneQuery(rosa.search.model.Query query) {
        if (query.getOperation() == null && query.getTerm() == null) {
            throw new IllegalArgumentException("Query must have operation or term");
        }
        
        if (query.isOperation() && query.children().length == 0) {
            throw new IllegalArgumentException("Query operation must have children");
        }
        
        if (query.isOperation()) {
            BooleanQuery result = new BooleanQuery();
            Occur occur = query.getOperation() == QueryOperation.AND ? Occur.MUST : Occur.SHOULD;

            for (rosa.search.model.Query kid : query.children()) {
                Query kid_query = createLuceneQuery(kid);

                if (kid_query != null) {
                    result.add(kid_query, occur);
                }
            }

            return result;
        } else {
            return create_lucene_query(query.getTerm());
        }
    }

    @Override
    public Query createLuceneQuery(String query) {
        try {
            return createLuceneQuery(QueryParser.parseQuery(query));
        } catch (ParseException e) {
        }

        BooleanQuery result = new BooleanQuery();

        for (SearchField sf : context_search_fields) {
            for (SearchFieldType type : sf.getFieldTypes()) {
                result.add(create_lucene_query(sf, type, query), Occur.SHOULD);
            }
        }

        return result;
    }

    private Query create_lucene_query(QueryTerm term) {
        SearchField sf = search_field_map.get(term.getField());

        if (sf == null) {
            throw new IllegalArgumentException("Unknown field: " + term.getField());
        }

        if (sf.getFieldTypes().length == 1) {
            return create_lucene_query(sf, sf.getFieldTypes()[0], term.getValue());
        } else {
            BooleanQuery query = new BooleanQuery();

            for (SearchFieldType type : sf.getFieldTypes()) {
                query.add(create_lucene_query(sf, type, term.getValue()), Occur.SHOULD);
            }

            return query;
        }
    }

    private Query create_lucene_query(SearchField sf, SearchFieldType type, String query) {
        String lucene_field = getLuceneField(sf, type);

        SimpleQueryParser parser = new SimpleQueryParser(main_analyzer, lucene_field);

        if (type == SearchFieldType.STRING) {
            return new TermQuery(new Term(lucene_field, query));
        } else {
            return parser.parse(query);
        }
    }

    protected void addField(Document doc, SearchField sf, SearchFieldType type, String value) {
        if (value == null) {
            return;
        }

        value = value.trim();

        if (value.isEmpty()) {
            return;
        }

        doc.add(create_field(getLuceneField(sf, type), type, value));
    }

    // Add field for each type. Use sparingly.
    protected void addField(Document doc, SearchField sf, String value) {
        for (SearchFieldType type : sf.getFieldTypes()) {
            addField(doc, sf, type, value);
        }
    }

    private IndexableField create_field(String name, SearchFieldType type, String value) {
        if (type == SearchFieldType.STRING) {
            return new StringField(name, value, Store.YES);
        } else {
            return new TextField(name, value, Store.YES);
        }
    }

    protected SearchFieldType getSearchFieldTypeForLang(String lc) {
        lc = lc.toLowerCase();

        switch (lc) {
            case "en":
                return SearchFieldType.ENGLISH;
            case "fr":
                return SearchFieldType.FRENCH;
            case "el":
                return SearchFieldType.GREEK;
            case "it":
                return SearchFieldType.ITALIAN;
            case "la":
                return SearchFieldType.LATIN;
            case "es":
                return SearchFieldType.SPANISH;
            default:
                return null;
        }
    }

    public String getSearchFieldNameFromLuceneField(String lucene_field) {
        int i = lucene_field.lastIndexOf('.');

        if (i == -1) {
            return lucene_field;
        }

        return lucene_field.substring(0, i);
    }

    public Set<String> getLuceneContextFields(rosa.search.model.Query query) {
        Set<String> result = new HashSet<>();
        get_lucene_context_fields(result, query);
        return result;
    }

    private void get_lucene_context_fields(Set<String> result, rosa.search.model.Query query) {
        if (query.isOperation()) {
            for (rosa.search.model.Query kid : query.children()) {
                get_lucene_context_fields(result, kid);
            }
        } else {
            SearchField sf = search_field_map.get(query.getTerm().getField());

            if (sf != null && sf.isContext()) {
                for (SearchFieldType type : sf.getFieldTypes()) {
                    result.add(getLuceneField(sf, type));
                }
            }
        }
    }

    public List<SearchField> getIncludeValueSearchFields() {
        return included_search_fields;
    }
}
