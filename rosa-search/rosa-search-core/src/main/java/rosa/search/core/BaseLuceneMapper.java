package rosa.search.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.el.GreekAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.pattern.PatternTokenizer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.QueryBuilder;

import rosa.search.model.QueryOperation;
import rosa.search.model.QueryTerm;
import rosa.search.model.SearchField;
import rosa.search.model.SearchFieldType;

/**
 * Handle mapping between search fields and Lucene fields, search queries and
 * Lucene queries.
 */
public abstract class BaseLuceneMapper implements LuceneMapper {

    private final Analyzer english_analyzer;
    private final Analyzer french_analyzer;
    private final Analyzer italian_analyzer;
    private final Analyzer spanish_analyzer;
    private final Analyzer greek_analyzer;
    private final Analyzer latin_analyzer;
    private final Analyzer imagename_analyzer;
    private final Analyzer string_analyzer;
    private final Analyzer main_analyzer;

    // TODO No special handling for old french spelling or character name variants

    // Lucene field name -> search field type
    private final Map<String, SearchFieldType> lucene_field_map;

    // Search field name -> search field
    private final Map<String, SearchField> search_field_map;

    
    public BaseLuceneMapper(SearchField... fields) {
        this.english_analyzer = new EnglishAnalyzer();
        this.french_analyzer = new FrenchAnalyzer();
        this.greek_analyzer = new GreekAnalyzer();
        this.italian_analyzer = new ItalianAnalyzer();
        this.spanish_analyzer = new SpanishAnalyzer();
        this.string_analyzer = new WhitespaceAnalyzer();
        
        // TODO Wrong. At least do latin stopwords.
        this.latin_analyzer = new StandardAnalyzer();
        
        // Tokenizes on spaces and . while removing excess 0's
        // TODO r/v?
        this.imagename_analyzer = new Analyzer() {
            Pattern pattern = Pattern.compile("\\s+|^0*|\\.0*");

            @Override
            protected TokenStreamComponents createComponents(String arg0) {
                Tokenizer tokenizer = new PatternTokenizer(pattern, -1);
                TokenStream filter = new LowerCaseFilter(tokenizer);

                return new TokenStreamComponents(tokenizer, filter);
            }
        };

        this.lucene_field_map = new HashMap<>();
        this.search_field_map = new HashMap<>();

        Map<String, Analyzer> analyzer_map = new HashMap<>();

        for (SearchField sf: fields) {
            search_field_map.put(sf.getFieldName(), sf);

            for (SearchFieldType type: sf.getFieldTypes()) {
                String lucene_field = getLuceneField(sf, type);

                lucene_field_map.put(lucene_field, type);
                analyzer_map.put(lucene_field, get_analyzer(type));
            }
        }

        this.main_analyzer = new PerFieldAnalyzerWrapper(string_analyzer,
                analyzer_map);
    }

    public String getLuceneField(SearchField sf, SearchFieldType type) {
        return sf.getFieldName() + "." + type.name();
    }

    private Analyzer get_analyzer(SearchFieldType type) {
        switch (type) {
        case ENGLISH:
            return english_analyzer;
        case FRENCH:
            return french_analyzer;
        case IMAGE_NAME:
            return imagename_analyzer;
        case OLD_FRENCH:
            return french_analyzer;
        case STRING:
            return string_analyzer;
        case SPANISH:
            return spanish_analyzer;
        case ITALIAN:
            return italian_analyzer;
        case GREEK:
            return greek_analyzer;
        case LATIN:
            return latin_analyzer;
        default:
            return null;
        }
    }

    public Analyzer getAnalyzer() {
        return main_analyzer;
    }

    public Query createLuceneQuery(rosa.search.model.Query query) {
        if (query.isOperation()) {
            BooleanQuery result = new BooleanQuery();
            Occur occur = query.getOperation() == QueryOperation.AND ? Occur.MUST
                    : Occur.SHOULD;

            for (rosa.search.model.Query kid: query.children()) {
                result.add(createLuceneQuery(kid), occur);
            }

            return result;
        } else {
            return create_lucene_query(query.getTerm());
        }
    }

    private Query create_lucene_query(QueryTerm term) {
        SearchField sf = search_field_map.get(term.getField());

        if (sf == null) {
            // TODO
            return null;
        }

        if (sf.getFieldTypes().length == 1) {
            return create_lucene_query(sf, sf.getFieldTypes()[0],
                    term.getValue());
        } else {
            BooleanQuery query = new BooleanQuery();

            for (SearchFieldType type: sf.getFieldTypes()) {
                Query q = create_lucene_query(sf, type, term.getValue());

                if (q != null) {
                    query.add(q, Occur.SHOULD);
                }
            }

            return query;
        }
    }

    // TODO Consider using SimpleQueryParser so fuzzy searchers etc are
    // supported?

    private Query create_lucene_query(SearchField sf, SearchFieldType type,
            String query) {
        String lucene_field = getLuceneField(sf, type);
        QueryBuilder builder = new QueryBuilder(main_analyzer);

        switch (type) {
        case ENGLISH:
            return builder.createPhraseQuery(lucene_field, query);
        case FRENCH:
            return builder.createPhraseQuery(lucene_field, query);
        case IMAGE_NAME:
            return builder.createPhraseQuery(lucene_field, query);
        case OLD_FRENCH:
            return builder.createPhraseQuery(lucene_field, query);
        case STRING:
            // Cannot do a phrase search on a String field.
            return new TermQuery(new Term(lucene_field, query));
        default:
            return null;
        }
    }

    // TODO Only use this when we do not know the language!!    
    protected void addField(Document doc, SearchField sf, String value) {
        for (SearchFieldType type: sf.getFieldTypes()) {
            doc.add(create_field(getLuceneField(sf, type), type, value));
        }
    }

    protected void addField(Document doc, SearchField sf, SearchFieldType type,
            String value) {
        doc.add(create_field(getLuceneField(sf, type), type, value));
    }

    private IndexableField create_field(String name, SearchFieldType type,
            String value) {
        switch (type) {
        case ENGLISH: case FRENCH: case OLD_FRENCH: case SPANISH:case LATIN: case ITALIAN: case GREEK:
            return new TextField(name, value, Store.YES);
        case IMAGE_NAME:
            return new TextField(name, value, Store.YES);
        case STRING:
            return new StringField(name, value, Store.YES);
        default:
            return null;
        }
    }

    protected SearchFieldType getSearchFieldTypeForLang(String lc) {
        lc = lc.toLowerCase();
        
        if (lc.equals("en")) {
            return SearchFieldType.ENGLISH;
        } else if (lc.equals("fr")) {
            return SearchFieldType.FRENCH;
        } else if (lc.equals("el")) {
            return SearchFieldType.GREEK;
        } else if (lc.equals("it")) {
            return SearchFieldType.ITALIAN;
        } else if (lc.equals("la")) {
            return SearchFieldType.LATIN;
        } else if (lc.equals("es")) {
            return SearchFieldType.SPANISH;
        } else {
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

    public Set<String> getLuceneFields(rosa.search.model.Query query) {
        Set<String> result = new HashSet<>();
        get_lucene_fields(result, query);
        return result;
    }

    private void get_lucene_fields(Set<String> result,
            rosa.search.model.Query query) {
        if (query.isOperation()) {
            for (rosa.search.model.Query kid: query.children()) {
                get_lucene_fields(result, kid);
            }
        } else {
            SearchField sf = search_field_map.get(query.getTerm().getField());

            if (sf != null) {
                for (SearchFieldType type: sf.getFieldTypes()) {
                    result.add(getLuceneField(sf, type));
                }
            }
        }
    }    
}
