package rosa.search.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherManager;
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
import rosa.search.model.SearchFields;
import rosa.search.model.SearchMatch;
import rosa.search.model.SearchOptions;
import rosa.search.model.SearchResult;

// TODO Where/how/if to deal with things like character variants and spelling variants?// 

/**
 * 
 * 
 *
 */
public class LuceneSearchService implements SearchService {
    private final static Logger logger = Logger
            .getLogger(LuceneSearchService.class.getName());

    private final Directory dir;
    private final SearcherManager searcher_manager;
    private final LuceneMapper mapper;
    private final String lucene_id_field;

    public LuceneSearchService(Path path) throws IOException {
        this.mapper = new LuceneMapper();
        this.dir = FSDirectory.open(path);

        // Create index if it does not exist
        try (IndexWriter iw = get_index_writer(false)) {
        }

        this.searcher_manager = new SearcherManager(dir, null);
        this.lucene_id_field = mapper.getLuceneField(SearchFields.ID,
                SearchFields.ID.getFieldTypes()[0]);
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

    private String create_resume_token(ScoreDoc last) {
        if (last == null) {
            return null;
        } else {
            return String.valueOf(last.doc) + "," + String.valueOf(last.score)
                    + "," + String.valueOf(last.shardIndex);
        }
    }

    private ScoreDoc parse_resume_token(String s) {
        if (s == null) {
            return null;
        }

        String[] parts = s.split(",");

        try {
            int doc = Integer.parseInt(parts[0]);
            float score = Float.parseFloat(parts[1]);
            int shard = Integer.parseInt(parts[2]);

            return new ScoreDoc(doc, score, shard);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<String> get_match_context(Highlighter hilighter, Document doc,
            Set<String> q_fields) throws IOException {
        List<String> context = new ArrayList<>();

        for (IndexableField field: doc) {
            if (field.fieldType().stored() && q_fields.contains(field.name())) {
                try {
                    String s = hilighter.getBestFragment(mapper.getAnalyzer(),
                            field.name(), field.stringValue());

                    if (s != null) {
                        context.add(mapper
                                .getSearchFieldNameFromLuceneField(field.name()));
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
    public SearchResult search(Query query, SearchOptions opts)
            throws IOException {
        searcher_manager.maybeRefresh();

        IndexSearcher searcher = searcher_manager.acquire();

        try {
            org.apache.lucene.search.Query q = mapper.createLuceneQuery(query);

            if (opts == null) {
                opts = new SearchOptions();
            }

            long offset = opts.getOffset();
            String resume_token = opts.getResumeToken();

            TopDocs hits;
            int hits_offset;

            if (resume_token == null) {
                // Degrade to retrieving all matches up to last requested.

                if (offset == 0) {
                    hits = searcher.search(q, opts.getMatchCount());
                    hits_offset = 0;
                } else {
                    // TODO Do multiple small searches instead?

                    if (offset > Integer.MAX_VALUE) {
                        throw new IllegalStateException("Offset too large: "
                                + offset);
                    }

                    hits = searcher.search(q, (int) offset + opts.getMatchCount());
                    hits_offset = (int) offset;
                }
            } else {
                ScoreDoc after = parse_resume_token(resume_token);

                if (after == null) {
                    throw new IllegalArgumentException("Invalid resume token: "
                            + opts.getResumeToken());
                }

                hits = searcher.searchAfter(after, q, opts.getMatchCount());
                hits_offset = 0;
            }

            Highlighter hilighter = new Highlighter(new QueryScorer(q));

            Set<Term> q_terms = new HashSet<>();
            q.extractTerms(q_terms);

            Set<String> q_fields = new HashSet<>();
            for (Term term: q_terms) {
                q_fields.add(term.field());
            }

            SearchMatch[] matches = new SearchMatch[hits.scoreDocs.length
                    - hits_offset];

            for (int i = hits_offset; i < hits.scoreDocs.length; i++) {
                Document doc = searcher.doc(hits.scoreDocs[i].doc);

                String id = doc.get(lucene_id_field);

                List<String> context = get_match_context(hilighter, doc,
                        q_fields);

                matches[i - hits_offset] = new SearchMatch(id, context);
            }

            ScoreDoc last;
            long total = hits.totalHits;

            if (matches.length > 0 && offset + matches.length < total) {
                last = hits.scoreDocs[hits.scoreDocs.length - 1];
            } else {
                last = null;
            }

            resume_token = create_resume_token(last);

            return new SearchResult(offset, total, matches, resume_token);
        } finally {
            searcher_manager.release(searcher);
        }
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
            logger.log(Level.WARNING,
                    "Error shutting down LuceneSearchService", e);
        }
    }

    public String getId(Document doc) {
        return doc.get(lucene_id_field);
    }

    @Override
    public void update(Store store, String collection_id) throws IOException {
        try (IndexWriter writer = get_index_writer(false)) {
            List<String> errors = new ArrayList<>();

            BookCollection col = store
                    .loadBookCollection(collection_id, errors);

            if (errors.size() > 0) {
                logger.warning("Errors loading collection: " + collection_id);

                for (String error: errors) {
                    logger.warning("  " + error);
                }
            }

            for (String book_id: col.books()) {
                errors.clear();

                Book book = store.loadBook(col, book_id, errors);

                if (errors.size() > 0) {
                    logger.warning("Errors loading book: " + book_id);

                    for (String error: errors) {
                        logger.warning("  " + error);
                    }
                }

                for (Document doc: mapper.createDocuments(col, book)) {
                    writer.updateDocument(
                            new Term(lucene_id_field, getId(doc)), doc);
                }
            }

            writer.forceMerge(1);
        }

        searcher_manager.maybeRefresh();
    }
}
