package rosa.iiif.presentation.core.search;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import rosa.archive.core.Store;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
import rosa.iiif.presentation.model.IIIFNames;
import rosa.iiif.presentation.model.PresentationRequest;
import rosa.iiif.presentation.model.PresentationRequestType;
import rosa.iiif.presentation.model.Reference;
import rosa.iiif.presentation.model.TextValue;
import rosa.iiif.presentation.model.annotation.Annotation;
import rosa.iiif.presentation.model.annotation.AnnotationTarget;
import rosa.iiif.presentation.model.search.IIIFSearchHit;
import rosa.iiif.presentation.model.search.IIIFSearchRequest;
import rosa.iiif.presentation.model.search.IIIFSearchResult;
import rosa.iiif.presentation.model.search.Rectangle;
import rosa.search.core.LuceneSearchService;
import rosa.search.core.SearchUtil;
import rosa.search.model.Query;
import rosa.search.model.QueryOperation;
import rosa.search.model.SearchMatch;
import rosa.search.model.SearchOptions;
import rosa.search.model.SearchResult;


// TODO Incomplete. Needs refactoring, documentation, and more testing.

public class LuceneIIIFSearchService extends LuceneSearchService implements IIIFSearchService, IIIFNames {
    private static final String[] IGNORED =  new String[] {"date", "user", "box"};
    
    private final IIIFPresentationRequestFormatter requestFormatter;

    public LuceneIIIFSearchService(Path index, IIIFPresentationRequestFormatter requestFormatter) throws IOException {
        super(index, new IIIFSearchLuceneMapper());
        
        this.requestFormatter = requestFormatter;
    }

    @Override
    public IIIFSearchResult search(IIIFSearchRequest request) throws IOException {
        SearchOptions options = new SearchOptions();
        options.setOffset(request.page);
        options.setMatchCount(Integer.MAX_VALUE);

        SearchResult sr = search(iiifToLuceneQuery(request), options);
        
        IIIFSearchResult result = luceneResultToIIIF(sr);
        result.setId(requestFormatter.format(request));

        result.setIgnored(IGNORED);
                        
        // TODO do fields: prev, next, first, last where applicable

        return result;
    }

    /**
     * @param newPage new results page number
     * @param original original request to copy
     * @return a copy of the original request with the results page modified
     */
    public IIIFSearchRequest copyChangePage(int newPage, IIIFSearchRequest original) {
        return new IIIFSearchRequest(
                original.objectId,
                arrayToString(original.queryTerms),
                arrayToString(original.motivations),
                arrayToString(original.dates),
                arrayToString(original.users),
                arrayToString(original.box),
                newPage
        );
    }

    /**
     * @param arr array of string values
     * @return a single String composed of all values in the original array
     *         separated by spaces
     */
    private String arrayToString(String[] arr) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < arr.length; i++) {
            if (i != 0) {
                sb.append(" ");
            }
            sb.append(arr[i]);
        }

        return sb.toString();
    }

    /**
     * @param arr array of rectangles
     * @return a single String of rectangles, separated by spaces, formatted: x,y,w,h
     */
    private String arrayToString(Rectangle[] arr) {
        if (arr == null || arr.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i != 0) {
                sb.append(" ");
            }
            sb.append(String.valueOf(arr[i].x));
            sb.append(',');
            sb.append(String.valueOf(arr[i].y));
            sb.append(',');
            sb.append(String.valueOf(arr[i].width));
            sb.append(',');
            sb.append(String.valueOf(arr[i].height));
        }
        return sb.toString();
    }
    

    /**
     * Transform a IIIF search request to a Lucene search query.
     *
     * @param iiifReq IIIF Search request
     * @return a Lucene query
     */
    protected Query iiifToLuceneQuery(IIIFSearchRequest iiifReq) {
        List<Query> top_query = new ArrayList<>();
        
        // TODO Rethink and redo all query parsing.
        
        String query = String.join(" ", iiifReq.queryTerms);

        List<String> terms = new ArrayList<>();
        
        for (String part: query.split("&")) {
            part = part.trim();
            
            if (!part.isEmpty()) {
                terms.add(part);
            }
        }
        
        Query text_query = new Query(QueryOperation.AND, new Query[terms.size()]);
        
        for (int i = 0; i < terms.size(); i++) {
            text_query.children()[i] = new Query(IIIFSearchFields.TEXT, terms.get(i));
        }
        
        top_query.add(text_query);
        
        // TODO Handle not having viable search
        
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
                top_query.add(new Query(IIIFSearchFields.IMAGE, getCanvasName(req)));
            case MANIFEST:
                top_query.add(new Query(IIIFSearchFields.BOOK, getManifestName(req)));
                break;
            case COLLECTION:
                top_query.add(new Query(IIIFSearchFields.COLLECTION, getCollectionName(req)));
                break;
            default:
                break;
        }

        Query result = new Query(QueryOperation.AND, top_query.toArray(new Query[]{}));
        
        return result;
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
    protected IIIFSearchResult luceneResultToIIIF(SearchResult result) throws IOException {
        IIIFSearchResult iiifResult = new IIIFSearchResult();

        // TODO assign proper search API (URI) IDs
        iiifResult.setTotal(result.getTotal());
        iiifResult.setStartIndex(result.getOffset());

        List<Annotation> annotations = iiifResult.getAnnotations();
        List<IIIFSearchHit> hits = new ArrayList<>();
        
        for (SearchMatch match : result.getMatches()) {
            String matchId = match.getId();
            String collection_id = getCollectionId(matchId);
            String book_id = getBookId(matchId);
            
            String annotation_id = gen_uri(collection_id, book_id, getAnnotationId(matchId), PresentationRequestType.ANNOTATION);
            String canvas_id = gen_uri(collection_id, book_id, getImageId(matchId), PresentationRequestType.CANVAS);
            String manifest_id = gen_uri(collection_id, book_id, null, PresentationRequestType.MANIFEST);
            
            // TODO Use new facility for returning field values in match
            // String[] labels = lookup(match, IIIFSearchFields.LABEL, IIIFSearchFields.TARGET_LABEL);
            
            // Reference manifest_ref = new Reference(manifest_id, new TextValue(labels[1], "en"), SC_MANIFEST); 
            
            Annotation anno = new Annotation();
            
            anno.setId(annotation_id);
            anno.setType(IIIFNames.OA_ANNOTATION);
            anno.setMotivation(IIIFNames.SC_PAINTING);
            
            // anno.setLabel(labels[0], "en");
            
            AnnotationTarget target = new AnnotationTarget();
            
            target.setUri(canvas_id);
            // target.setParentRef(manifest_ref);            
            
            anno.setDefaultTarget(target);
            
            annotations.add(anno);
            
            hits.addAll(getContextHits(match.getContext(), matchId, collection_id, book_id));
        }

        // TODO Just use array list?
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
     * <pre>
     *   Lucene context: "This is a <b>matching string</b> with context"
     *   IIIF Hit: {
     *       match: "matching string",
     *       before: "This is a ",
     *       after: " with context"
     *   }
     * }
     * </pre>
     * 
     * One minor complication is the possibility that if sequential words are matched,
     * Lucene will often surround the individual words with HTML 'b' tags, instead of
     * the phrase: EX: {@code A string <b>with</b> <b>multiple</b> matches}. In this case,
     * consecutive bold words should be detected and be put together in the same IIIF
     * 'match' parameter, instead of separate IIIF hits.
     *
     * EX:
     * <pre>
     *   Lucene context: "This is a <b>matching</b> <b>string</b> with context"
     *   IIIF Hit: {
     *       match: "matching string",
     *       before: "This is a ",
     *       after: " with context"
     *   }
     * </pre>
     *
     * TODO: Selectors
     * If multiple Hits are found for the same annotation, they should both be included
     * in the same IIIFSearchHit object, but as separate selectors.
     *
     * <pre>
     *   Lucene context: "This context has <b>multiple</b> separate <b>matching</b> parts"
     *   
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
     * </pre>
     * 
     * @param contexts list of search contexts
     * @param matchId ID field from a search match
     * @return a list of IIIF Hits
     */
    protected List<IIIFSearchHit> getContextHits(List<String> contexts, String matchId,
                                                 String collection, String book) {
        List<IIIFSearchHit> hits = new ArrayList<>();
        
        String[] associatedAnnos = new String[] {
                gen_uri(collection, book, getAnnotationId(matchId), PresentationRequestType.ANNOTATION)
        };

        // TODO Does not deal with nested <B>
        
        final String start_tag = "<B>";
        final String end_tag = "</B>";
        
        for (int i = 0; i < contexts.size();) {
            String field = contexts.get(i++);
            String context = contexts.get(i++);
            
            if (!field.equals(IIIFSearchFields.TEXT.name())) {
                continue;
            }
            
            int start = context.indexOf(start_tag);
            int end = 0;
            
            while (start >= 0 && start < context.length()) {
                String hit_before = context.substring((end == 0 ? end : end+4), start);
                end = context.indexOf(end_tag, start);
                String hit_match = context.substring(start + 3, end);
                start = context.indexOf(start_tag, end);

                String hit_after;
                if (start > context.length() || start < 0) {
                    hit_after = context.substring(end + 4);
                } else {
                    hit_after = context.substring(end + 4, start);
                }

                hits.add(new IIIFSearchHit(associatedAnnos, hit_match, hit_before, hit_after));
            }
        }


        return hits;
    }
    
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

    private String gen_uri(String collection, String book, String name, PresentationRequestType type) {
        return requestFormatter.format(presentationRequest(collection, book, name, type));
    }

    private String presentationId(String collection, String book) {
        return collection + (book == null || book.isEmpty() ? "" : "." + book);
    }

    private PresentationRequest presentationRequest(String collection, String book, String name,
                                                    PresentationRequestType type) {
        return new PresentationRequest(presentationId(collection, book), name, type);
    }

    private String getCollectionId(String matchId) {
        return SearchUtil.getCollectionFromId(matchId);
    }

    private String getBookId(String matchId) {
        return SearchUtil.getBookFromId(matchId);
    }
    
    private String getImageId(String matchId) {
        return SearchUtil.getImageFromId(matchId);
    }

    private String getAnnotationId(String matchId) {
        return SearchUtil.getAnnotationFromId(matchId);
    }

    @Override
    public void update(Store store) throws IOException {
        for (String col: store.listBookCollections()) {
            update(store, col);
        }
    }
}
