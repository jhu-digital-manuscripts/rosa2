package rosa.search.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.document.Document;
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
import rosa.search.model.Query;
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
        searcher_manager.maybeRefresh();

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

            long offset = opts.getOffset();

            TopDocs hits;
            int hits_offset;

            SortOrder sort_order = opts.getSortOrder();
            Sort lucene_order;
            
            if (opts.getSortOrder() == SortOrder.INDEX) {
            	lucene_order  = Sort.INDEXORDER;
            } else {
            	lucene_order = Sort.RELEVANCE;
            }
            
            if (offset == 0) {

            	hits = searcher.search(q, opts.getMatchCount(), lucene_order);
            	hits_offset = 0;
            } else {
            	if (offset > Integer.MAX_VALUE) {
                	// TODO Do multiple small searches instead?
            		throw new IllegalStateException("Offset too large: " + offset);
            	}

            	hits = searcher.search(q, (int) offset + opts.getMatchCount(), lucene_order);
            	hits_offset = (int) offset;
            }
           
            Highlighter hilighter = new Highlighter(new QueryScorer(q));

            Set<String> query_context_fields = mapper.getLuceneContextFields(query);

            SearchMatch[] matches = new SearchMatch[hits.scoreDocs.length - hits_offset];

            for (int i = hits_offset; i < hits.scoreDocs.length; i++) {
                int doc_id = hits.scoreDocs[i].doc;

                Document doc = searcher.doc(doc_id);

                String id = doc.get(lucene_id_field);

                List<String> context = get_match_context(hilighter, doc, query_context_fields);
                List<String> values = get_match_values(doc);

                matches[i - hits_offset] = new SearchMatch(id, context, values);
            }

            long total = hits.totalHits;
            

            return new SearchResult(offset, total, matches, sort_order, q.toString());
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

    @Override
    public void clear() throws IOException {
        try (IndexWriter iw = get_index_writer(true)) {
        }

        searcher_manager.maybeRefresh();
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
