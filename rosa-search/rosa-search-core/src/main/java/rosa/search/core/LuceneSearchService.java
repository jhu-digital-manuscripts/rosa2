package rosa.search.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.document.Document;
import org.apache.lucene.facet.DrillDownQuery;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.facet.sortedset.DefaultSortedSetDocValuesReaderState;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetCounts;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesReaderState;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import rosa.archive.core.Store;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.search.model.CategoryValueCount;
import rosa.search.model.Query;
import rosa.search.model.QueryTerm;
import rosa.search.model.SearchCategoryMatch;
import rosa.search.model.SearchField;
import rosa.search.model.SearchMatch;
import rosa.search.model.SearchOptions;
import rosa.search.model.SearchResult;
import rosa.search.model.SortOrder;

/**
 * Implementation of the search service using Lucene. It relies on the LuceneMapper abstraction to handle
 * some of the details of creating lucene queries and indexing.
 */
public class LuceneSearchService implements SearchService {
    private final static Logger logger = Logger.getLogger(LuceneSearchService.class.getName());

    private final Directory dir;
    private final SearcherManager searcher_manager;
    private final LuceneMapper mapper;
    private final String lucene_id_field;

    public LuceneSearchService(Path path, LuceneMapper mapper) throws IOException {
        this.mapper = mapper;
        this.dir = FSDirectory.open(path);

        // Create index if it does not exist
        try (IndexWriter iw = get_index_writer(false)) {
        }

        this.searcher_manager = new SearcherManager(dir, null);

        SearchField id_field = mapper.getIdentifierSearchField();

        this.lucene_id_field = mapper.getLuceneField(id_field, id_field.getFieldTypes()[0]);
    }

    private IndexWriter get_index_writer(boolean create) throws IOException {
        IndexWriterConfig iwc = new IndexWriterConfig(mapper.getAnalyzer());

        if (create) {
            iwc.setOpenMode(OpenMode.CREATE);
        } else {
            iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
        }

        iwc.setRAMBufferSizeMB(256);

        return new IndexWriter(dir, iwc);
    }

    private List<String> get_match_context(Highlighter hilighter, Document doc, Set<String> context_fields)
            throws IOException {
        List<String> context = new ArrayList<>();

        for (String lucene_field : context_fields) {
            for (String value : doc.getValues(lucene_field)) {
                try {
                    String s = hilighter.getBestFragment(mapper.getAnalyzer(), lucene_field, value);

                    if (s != null) {
                        context.add(mapper.getSearchFieldNameFromLuceneField(lucene_field));
                        context.add(s);
                    }
                } catch (InvalidTokenOffsetsException e) {
                    continue;
                }
            }
        }

        return context;
    }

    @Override
    public SearchResult search(Query query, SearchOptions opts) throws IOException {
        IndexSearcher searcher = searcher_manager.acquire();

        try {
            org.apache.lucene.search.Query q = mapper.createLuceneQuery(query);

            if (q == null) {
                // TODO Throw invalid argument exception?
                return new SearchResult();
            }
            
            if (opts == null) {
                opts = new SearchOptions();
            }
            
            TopDocs hits;
            List<SearchCategoryMatch> category_matches;
            
            SortOrder sort_order = opts.getSortOrder();
            Sort lucene_order;
            
            if (opts.getSortOrder() == SortOrder.INDEX) {
            	lucene_order  = Sort.INDEXORDER;
            } else {
            	lucene_order = Sort.RELEVANCE;
            }

            if (opts.getOffset() > Integer.MAX_VALUE) {
                // TODO Do multiple small searches instead?
                throw new IllegalStateException("Offset too large: " + opts.getOffset());
            }
            
            int offset = (int) opts.getOffset();
            
            if (opts.getCategories() != null) {
                // Categories indicate a faceted search

                FacetsCollector fc = new FacetsCollector(true);
                SortedSetDocValuesReaderState state = new DefaultSortedSetDocValuesReaderState(searcher.getIndexReader());
                
                hits = FacetsCollector.search(searcher, create_dill_down_query(opts.getCategories(), q), null,
                        offset + opts.getMatchCount(), lucene_order, fc);
                category_matches = get_category_matches(fc, state);
            } else {
                hits = searcher.search(q, offset + opts.getMatchCount(), lucene_order);
                category_matches = null;
            }
            
            Highlighter hilighter = new Highlighter(new QueryScorer(q));

            Set<String> query_context_fields = mapper.getLuceneContextFields(query);

            SearchMatch[] matches = new SearchMatch[hits.scoreDocs.length - offset];

            for (int i = offset; i < hits.scoreDocs.length; i++) {
                int doc_id = hits.scoreDocs[i].doc;

                Document doc = searcher.doc(doc_id);

                String id = doc.get(lucene_id_field);

                List<String> context = get_match_context(hilighter, doc, query_context_fields);
                List<String> values = get_match_values(doc);

                matches[i - offset] = new SearchMatch(id, context, values);
            }

            return new SearchResult(offset, hits.totalHits, opts.getMatchCount(), matches, sort_order, q.toString(), category_matches);

        } finally {
            searcher_manager.release(searcher);
        }
    }

    private List<String> get_match_values(Document doc) {
        List<String> values = new ArrayList<>();

        for (SearchField sf : mapper.getIncludeValueSearchFields()) {
            String lucene_field = mapper.getLuceneField(sf, sf.getFieldTypes()[0]);

            for (String value : doc.getValues(lucene_field)) {
                values.add(sf.getFieldName());
                values.add(value);
            }
        }

        return values;
    }
    
    // Create a facet query from a set of category values and a base query
    private DrillDownQuery create_dill_down_query(List<QueryTerm> category_terms, org.apache.lucene.search.Query baseQuery) {
        DrillDownQuery dq = new DrillDownQuery(mapper.getFacetsConfig(), baseQuery);
        
        category_terms.forEach(t -> {
            // Empty or null value indicates match all values
            if (t.getValue() == null || t.getValue().isEmpty()) {
                dq.add(t.getField());
            } else {
                dq.add(t.getField(), t.getValue());
            }
        });
        
        return dq;
    }

    /**
     * Get the category matches from the facets collected from a search.
     *
     * IMPL NOTE: Using this method for getting category values relies on
     * {@link SortedSetDocValuesFacetCounts#getAllDims(int)} forcing us to
     * specify the maximum number of values that can be returned. Undocumented
     * is the maximum that this number can be:
     * <em>topN</em> must be less than org.apache.lucene.util.ArrayUtil.MAX_ARRAY_LENGTH,
     * which is Integer.MAX_VALUE - whatever overhead arrays require.
     *
     */
    private List<SearchCategoryMatch> get_category_matches(FacetsCollector fc, SortedSetDocValuesReaderState state) throws IOException {
        Facets facets = new SortedSetDocValuesFacetCounts(state, fc);

        List<SearchCategoryMatch> result = new ArrayList<>();

        int mamResults = searcher_manager.acquire().getIndexReader().numDocs();
        facets.getAllDims(mamResults).forEach(f -> result.add(get_category_matches(f)));

        result.sort((o1, o2) -> o1.getFieldName().compareToIgnoreCase(o2.getFieldName()));

        // Sort values in each category

        result.parallelStream().forEach(cat -> {
            Arrays.sort(cat.getValues(), (o1, o2) -> {
                return compare_strings_possibly_ending_with_numbers(o1.getValue(), o2.getValue());
            });
        });

        return result;
    }
    
    private static final int find_last_digit_sequence(String s) {
        int last = -1;

        for (int i = s.length() - 1; i >= 0; i--) {
            if (Character.isDigit(s.charAt(i))) {
                last = i;
            } else {
                break;
            }
        }

        return last;
    }

    // Sort in alphabetical order, but if identical prefixes end in numbers, order by those numbers.
    private static int compare_strings_possibly_ending_with_numbers(String s1, String s2) {
        int i1 = find_last_digit_sequence(s1);
        int i2 = find_last_digit_sequence(s2);

        if (i1 == -1 || i2 == -1) {
            return s1.compareTo(s2);
        }

        // Special case for values like number-number, compare by first number, then second
        // Else compare by prefix string, then number
        
        int compare;
        
        if (i1 > 0 && i2 > 0 && s1.charAt(i1 - 1) == '-' && s2.charAt(i2 -1) == '-') {
            int n1 = Integer.parseInt(s1.substring(0, i1 - 1));
            int n2 = Integer.parseInt(s2.substring(0, i2 - 1));

            compare = n1 - n2;   
        } else {
            compare = s1.substring(0, i1).compareTo(s2.substring(0, i2));
        }
        
        if (compare == 0) {
            int n1 = Integer.parseInt(s1.substring(i1));
            int n2 = Integer.parseInt(s2.substring(i2));

            return n1 - n2;
        }

        return compare;
    }

    // Get the values within a category together with corresponding coutns from a FacetResult
    private SearchCategoryMatch get_category_matches(FacetResult facet_result) {
        CategoryValueCount[] values = new CategoryValueCount[facet_result.labelValues.length];
        
        for (int i = 0; i < facet_result.labelValues.length; i++) {
            LabelAndValue lv = facet_result.labelValues[i];
            values[i] = new CategoryValueCount(lv.label, lv.value.intValue());
        }
        
        return new SearchCategoryMatch(facet_result.dim, values);
    }

    @Override
    public void clear() throws IOException {
        try (IndexWriter iw = get_index_writer(true)) {
        }

        searcher_manager.maybeRefresh();
    }

    
    @Override
    public boolean isEmpty() throws IOException {
        try (IndexWriter iw = get_index_writer(false)) {
        	return iw.numDocs() == 0;
        }
    }
    
    @Override
    public void shutdown() {
        try {
            searcher_manager.close();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error shutting down LuceneSearchService", e);
        }
    }

    private String get_id(Document doc) {
        return doc.get(lucene_id_field);
    }

    @Override
    public void update(Store store, String collection_id) throws IOException {
        try (IndexWriter writer = get_index_writer(false)) {
            List<String> errors = new ArrayList<>();

            BookCollection col = store.loadBookCollection(collection_id, errors);

            if (errors.size() > 0) {
                logger.warning("Errors loading collection: " + collection_id);

                for (String error : errors) {
                    logger.warning("  " + error);
                }
            }

            for (String book_id : col.books()) {
                errors.clear();

                Book book = store.loadBook(col, book_id, errors);

                if (errors.size() > 0) {
                    logger.warning("Errors loading book: " + book_id);

                    for (String error : errors) {
                        logger.warning("  " + error);
                    }
                }

                logger.info("Updating index for: [" + collection_id + ":" + book_id + "]");
                for (Document doc : mapper.createDocuments(col, book)) {
                    String id = get_id(doc);

                    if (id == null) {
                        logger.severe("Document does not have id: [" + collection_id + ":" + book_id + "]");
                        continue;
                    }

                    writer.updateDocument(new Term(lucene_id_field, id), doc);
                }
            }

            writer.forceMerge(1);
        }

        searcher_manager.maybeRefresh();
    }
}
