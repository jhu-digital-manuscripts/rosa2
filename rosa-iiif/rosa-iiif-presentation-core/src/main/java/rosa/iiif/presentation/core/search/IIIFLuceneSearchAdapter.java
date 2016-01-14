package rosa.iiif.presentation.core.search;

import rosa.archive.core.Store;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.iiif.presentation.core.IIIFRequestFormatter;
import rosa.iiif.presentation.core.transform.impl.AnnotationTransformer;
import rosa.archive.core.util.Annotations;
import rosa.iiif.presentation.model.IIIFNames;
import rosa.iiif.presentation.model.PresentationRequest;
import rosa.iiif.presentation.model.PresentationRequestType;
import rosa.iiif.presentation.model.Reference;
import rosa.iiif.presentation.model.TextValue;
import rosa.iiif.presentation.model.annotation.Annotation;
import rosa.iiif.presentation.model.search.IIIFSearchHit;
import rosa.iiif.presentation.model.search.IIIFSearchRequest;
import rosa.iiif.presentation.model.search.IIIFSearchResult;
import rosa.iiif.presentation.model.search.SearchCategory;
import rosa.search.core.SearchUtil;
import rosa.search.model.Query;
import rosa.search.model.QueryOperation;
import rosa.search.model.SearchFields;
import rosa.search.model.SearchMatch;
import rosa.search.model.SearchResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adapt Lucene search results into JSON-LD that follows the IIIF Search API
 * (http://search.iiif.io/api/search/0.9/)
 *
 * Must be able to transform:
 *
 * - HTTP GET request formatted as specified in the IIIF Search API TO a Lucene
 * query that can be handed to the search service.
 *   - (http://search.iiif.io/api/search/0.9/#request)
 *
 * - Search results from the search service TO a format specified in the IIIF Search API
 *   - (http://search.iiif.io/api/search/0.9/#response)
 */
public class IIIFLuceneSearchAdapter implements IIIFNames {
    private static final int max_cache_size = 1000;

    private AnnotationTransformer annotationTransformer;
    private IIIFRequestFormatter presReqFormatter;
    private Store archiveStore;

    private final ConcurrentHashMap<String, Object> cache;

    public IIIFLuceneSearchAdapter(AnnotationTransformer annotationTransformer, Store archiveStore,
                                   IIIFRequestFormatter presReqFormatter) {
        this.annotationTransformer = annotationTransformer;
        this.presReqFormatter = presReqFormatter;
        this.archiveStore = archiveStore;

        this.cache = new ConcurrentHashMap<>(max_cache_size);
    }

    /**
     * Transform a IIIF search request to a Lucene search query.
     *
     * @param iiifReq IIIF Search request
     * @return a Lucene query
     */
    public Query iiifToLuceneQuery(IIIFSearchRequest iiifReq) {
        // This will generate independent queries for each word...
        List<Query> top_query = new ArrayList<>();

        StringBuilder query = new StringBuilder();
        for (int i = 0; i < iiifReq.queryTerms.length; i++) {
            if (i != 0) {
                query.append(' ');
            }
            query.append(iiifReq.queryTerms[i]);
        }

        if (!query.toString().isEmpty()) {
            List<Query> searchQuery = new ArrayList<>();
            for (SearchFields luceneField : SearchCategory.ALL.luceneFields) {
                searchQuery.add(new Query(luceneField, query.toString().trim()));
            }
            top_query.add(new Query(QueryOperation.OR, searchQuery.toArray(new Query[searchQuery.size()])));
        }

        /*
            Here, the rest of the parameters would be added to the query (motivation, date, user, box).

            In this specific case, motivation will be the same for all annotations that are being
            searched and will be omitted.

            The other parameters will not be searched for either, as no data is stored related
            to them. No user data, no time-stamp, location data is not precise enough for a box
            restriction.

            TODO to make this more general
            consider a generalized map: String (parameter) -> String[] (query terms)
            Parameters can be related (or the same as) Lucene fields. These can then be iterated
            over to generate the Lucene query.
         */

        // Restrict query based on requested object
        PresentationRequest req = iiifReq.objectId;
        switch (iiifReq.objectId.getType()) {
            case CANVAS:
                top_query.add(new Query(SearchFields.IMAGE_NAME, getCanvasName(req)));
            case MANIFEST:
                top_query.add(new Query(SearchFields.BOOK_ID, getManifestName(req)));
            case COLLECTION:
                top_query.add(new Query(SearchFields.COLLECTION_ID, getCollectionName(req)));
            default:
                break;
        }

        return new Query(QueryOperation.AND, top_query.toArray(new Query[top_query.size()]));
    }

    /**
     * Transform a Lucene search result into a IIIF search result. The Lucene
     * search result is structured contains the offset, total # results, and
     * an array of SearchMatches
     *
     * Lucene result match will contain an ID of the format:
     *      collection_name;book_name;page_id;(annotation_id)
     *
     * From this ID, the annotation can be retrieved from the archive and
     * transformed into IIIF format.
     *
     * The context gives a small snippet of text surrounding the search hit.
     * This context is handled with {@link #getContextHits(List, String, String, String)}
     *
     * @param result lucene result
     * @return IIIF compatible result
     * @throws IOException possible exception if the archive is unavailable
     *
     * @see SearchResult
     * @see rosa.search.model.SearchMatch
     */
    public IIIFSearchResult luceneResultToIIIF(SearchResult result) throws IOException {
        IIIFSearchResult iiifResult = new IIIFSearchResult();

        // TODO assign proper search API (URI) IDs
        iiifResult.setTotal(result.getTotal());
        iiifResult.setStartIndex(result.getOffset());

        // Create List<iiif annotation>
        List<Annotation> annotations = iiifResult.getAnnotations();
        List<IIIFSearchHit> hits = new ArrayList<>();
        for (SearchMatch match : result.getMatches()) {
            // Create the Annotation from Match
            //   Get archive annotation from match ID using Annotations util class
            String matchId = match.getId();

            BookCollection col = getCollection(matchId);
            Book book = getBook(matchId);
            if (book == null) {
                // TODO log if this happens
                continue;
            }
            rosa.archive.model.aor.Annotation archiveAnno = getArchiveAnnotation(matchId);
            if (archiveAnno == null) {
                continue;
            }
            archiveAnno.setId(SearchUtil.getAnnotationFromId(matchId));

            //   Transform archive anno -> iiif anno using AnnotationTransformer
            Annotation presentationAnno = annotationTransformer.transform(col, book, archiveAnno);



            // Set presentation annotation parent reference
            Reference parentRef = new Reference(
                    urlId(col.getId(), book.getId(), null, PresentationRequestType.MANIFEST),
                    new TextValue(book.getBookMetadata("en").getCommonName(), "en"),
                    SC_MANIFEST);        // TODO proper language support here
            presentationAnno.getDefaultTarget().setParentRef(parentRef);

            annotations.add(presentationAnno);
            hits.addAll(getContextHits(match.getContext(), matchId, col.getId(), book.getId()));
        }

        iiifResult.setHits(hits.toArray(new IIIFSearchHit[hits.size()]));

        /*
            TODO URIs for paging:
            First
            last
            next
            prev
         */

        return iiifResult;
    }

    /**
     * Translate the search results contexts into IIIF form.
     *
     * Search results from Lucene return with an ID and a set of strings that set the
     * context of the search match. In these context strings, those words that were
     * matched are bold using HTML &lt;b&gt; tags. Surrounding text is present to give
     * the match some context. These strings must be transformed into a form used by IIIF
     * results.
     *
     * Search results in IIIF are formatted according to the (new/WIP) sc:Hit object. In
     * this form, the search context can use an oa:TextQuoteSelector, which hold the
     * matching words in a parameter (match|exact) and holds surrounding contextual text
     * in separate parameters (before|prefix) and (after|suffix).
     *
     * When translating, all text surrounded by HTML 'b' tags are put in the 'matching'
     * parameter, while preceding text is put in the 'before' parameter and proceeding
     * text is put in the 'after' parameter.
     *
     * EX:
     * {@code
     *   Lucene context: "This is a <b>matching string</b> with context"
     *   IIIF Hit: {
     *       match: "matching string",
     *       before: "This is a ",
     *       after: " with context"
     *   }
     * }
     *
     * One minor complication is the possibility that if sequential words are matched,
     * Lucene will often surround the individual words with HTML 'b' tags, instead of
     * the phrase: EX: {@code A string <b>with</b> <b>multiple</b> matches}. In this case,
     * consecutive bold words should be detected and be put together in the same IIIF
     * 'match' parameter, instead of separate IIIF hits.
     *
     * EX:
     * {@code
     *   Lucene context: "This is a <b>matching</b> <b>string</b> with context"
     *   IIIF Hit: {
     *       match: "matching string",
     *       before: "This is a ",
     *       after: " with context"
     *   }
     * }
     *
     * TODO: Selectors
     * If multiple Hits are found for the same annotation, they should both be included
     * in the same IIIFSearchHit object, but as separate selectors.
     *
     * EX:
     * {@code
     *   Lucene context: "This context has <b>multiple</b> separate <b>matching</b> parts"
     *   IIIF Hit: {
     *       annotations: [ annoId ],
     *       selectors: [{
     *              match: "multiple",
     *              before: "This context has ",
     *              after: " separate "
     *          },{
     *              match: "matching",
     *              before: " separate ",
     *              after: " parts"
     *          }
     *       ]
     *   }
     * }
     *
     * @param contexts list of search contexts
     * @param matchId ID field from a search match
     * @return a list of IIIF Hits
     */
    protected List<IIIFSearchHit> getContextHits(List<String> contexts, String matchId,
                                                 String collection, String book) {
        List<IIIFSearchHit> hits = new ArrayList<>();
        String[] associatedAnnos = new String[] {
                urlId(collection, book, getAnnotationId(matchId), PresentationRequestType.ANNOTATION)
        };

        // Create Hit objects from the context
        for (String context : contexts) {
            String tmp = context.toLowerCase();     // In case <B> appears instead of <b>
            int start = tmp.indexOf("<b>");
            int end = 0;
            while (start >= 0 && start < tmp.length()) {
                String hit_before = context.substring((end == 0 ? end : end+4), start);
                end = tmp.indexOf("</b>", start);
                String hit_match = context.substring(start + 3, end);
                start = tmp.indexOf("<b>", end);

                String hit_after;
                if (start > context.length() || start < 0) {
                    hit_after = context.substring(end + 4);
                } else {
                    hit_after = context.substring(end + 4, start);
                }

                hits.add(new IIIFSearchHit(associatedAnnos, hit_match, hit_before, hit_after));
            }
        }

//        int i = 0;
//        while (i < hits.size() - 1) {
//            IIIFSearchHit hit1 = hits.get(i);
//            IIIFSearchHit hit2 = hits.get(i + 1);
//
//            if (isEmpty(hit1.after) && isEmpty(hit2.before) && !hit1.matching.equals("tif")) {
//                // If (after hit1) == (before hit2) == (blank), then merge the 2 hits
//                hits.remove(hit1);
//                hits.remove(hit2);
//                hits.add(i, new IIIFSearchHit(
//                        associatedAnnos, (hit1.matching + " " + hit2.matching), hit1.before, hit2.after
//                ));
//
//                i--;
//            }
//
//            i++;
//        }

        return hits;
    }

    private boolean isEmpty(String str) {
        return str == null || str.isEmpty() || str.matches("^\\s+$");
    }

    /**
     * TODO for now, assume ALL category. this will change when faceted search is implemented
     *
     * @param queryFragment query fragment in question
     * @return search category associated with the fragment
     */
    private SearchCategory getSearchCategory(String queryFragment) {
        return SearchCategory.ALL;
    }

    /**
     * TODO for now, just return the fragment. this may change when faceted search is implemented
     * @param queryFrag part of query term from request
     * @return keyword to search for
     */
    private String getSearchTerm(String queryFrag) {
        return queryFrag;
    }

    // -----  -----

    private String getCollectionName(PresentationRequest request) {
        switch (request.getType()) {
            case COLLECTION:
                return request.getName();
            default:
                String[] parts = request.getId().split("\\.");
                return parts[0];
        }
    }

    private String getManifestName(PresentationRequest request) {
        switch (request.getType()) {
            case COLLECTION:
                return null;
            default:
                String[] parts = request.getId().split("\\.");
                return parts[1];
        }
    }

    private String getCanvasName(PresentationRequest request) {
        switch (request.getType()) {
            case CANVAS:
                return request.getName();
            default:
                return null;
        }
    }

    // ----- URI parsing -----

    // TODO Shared code with BasePresentationTransformer. Externalize!
    protected String urlId(String collection, String book, String name, PresentationRequestType type) {
        return presReqFormatter.format(presentationRequest(collection, book, name, type));
    }

    private String presentationId(String collection, String book) {
        return collection + (book == null || book.isEmpty() ? "" : "." + book);
    }

    private PresentationRequest presentationRequest(String collection, String book, String name,
                                                    PresentationRequestType type) {
        return new PresentationRequest(presentationId(collection, book), name, type);
    }

    // ----- Name parsing -----

    private String getAnnotationCollection(String matchId) {
        return SearchUtil.getCollectionFromId(matchId);
    }

    private String getAnnotationBook(String matchId) {
        return SearchUtil.getBookFromId(matchId);
    }

//    private String getAnnotationPage(String matchId) {
//        return SearchUtil.getImageFromId(matchId);
//    }

    private String getAnnotationId(String matchId) {
        return SearchUtil.getAnnotationFromId(matchId);
    }

    // ----- Caching -----

    private String cache_key(String id, Class<?> type) {
        return id + "," + type.getName();
    }

    private <T> T lookupCache(String id, Class<T> type) {
        return type.cast(cache.get(cache_key(id, type)));
    }

    private void updateCache(String id, Object value) {
        if (id == null || value == null) {
            return;
        }

        if (cache.size() > max_cache_size) {
            cache.clear();
        }

        cache.putIfAbsent(cache_key(id, value.getClass()), value);
    }

    private BookCollection getCollection(String matchId) throws IOException {
        String col_id = getAnnotationCollection(matchId);
        BookCollection result = lookupCache(col_id, BookCollection.class);

        if (result == null) {
            result = archiveStore.loadBookCollection(col_id, null);
            updateCache(col_id, result);
        }

        return result;
    }

    private Book getBook(String matchId) throws IOException {
        String col_id = getAnnotationCollection(matchId);
        String book_id = getAnnotationBook(matchId);

        Book result = lookupCache(book_id, Book.class);

        if (result == null) {
            BookCollection col = getCollection(col_id);

            if (col == null) {
                return null;
            }

            result = archiveStore.loadBook(col, book_id, null);
            updateCache(book_id, result);
        }

        return result;
    }

    private rosa.archive.model.aor.Annotation getArchiveAnnotation(String matchId) throws IOException {
        String annoId = getAnnotationId(matchId);

        rosa.archive.model.aor.Annotation anno = lookupCache(annoId, rosa.archive.model.aor.Annotation.class);

        if (anno == null) {
            Book book = getBook(matchId);

            if (book == null) {
                return null;
            }

            anno = Annotations.getArchiveAnnotation(book, annoId);
            updateCache(annoId, rosa.archive.model.aor.Annotation.class);
        }

        return anno;
    }
}
