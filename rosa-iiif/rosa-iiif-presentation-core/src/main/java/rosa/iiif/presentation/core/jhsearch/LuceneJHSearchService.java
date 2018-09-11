package rosa.iiif.presentation.core.jhsearch;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import rosa.archive.core.Store;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
import rosa.iiif.presentation.model.PresentationRequest;
import rosa.iiif.presentation.model.PresentationRequestType;
import rosa.search.core.LuceneSearchService;
import rosa.search.core.ParseException;
import rosa.search.core.QueryParser;
import rosa.search.model.Query;
import rosa.search.model.QueryOperation;
import rosa.search.model.SearchOptions;
import rosa.search.model.SearchResult;
import rosa.search.model.SortOrder;

public class LuceneJHSearchService extends LuceneSearchService implements JHSearchService {
    private final JHSearchSerializer serializer;
    private final IIIFPresentationRequestFormatter formatter;
    
    // Map collection names to available search fields
    // TODO Make configurable in archive or move to properties file. Perhaps move to lucene mapper?
    private static final Map<String,JHSearchField[]> searchfields = new HashMap<>();
    private static final Map<String,JHSearchCategory[]> searchcategories = new HashMap<>();
       
    static {
        searchfields.put("rosecollection",
                new JHSearchField[] {
                        JHSearchField.DESCRIPTION,
                        JHSearchField.REPO,
                        JHSearchField.PLACE,
                        JHSearchField.ILLUSTRATION,
                        JHSearchField.CHAR_NAME,
                        JHSearchField.TRANSCRIPTION});
        searchcategories.put("rosecollection",
                new JHSearchCategory[] {JHSearchCategory.COMMON_NAME, JHSearchCategory.LOCATION, JHSearchCategory.DATE,
                        JHSearchCategory.NUM_ILLUS, JHSearchCategory.NUM_PAGES, JHSearchCategory.ORIGIN, JHSearchCategory.TYPE,
                        JHSearchCategory.TRANSCRIPTION});
        
        searchfields.put("pizancollection",
                new JHSearchField[] {
                        JHSearchField.DESCRIPTION,
                        JHSearchField.REPO,
                        JHSearchField.PLACE,
                        JHSearchField.TITLE,
                        JHSearchField.TRANSCRIPTION});
        searchcategories.put("pizancollection",
                new JHSearchCategory[] {JHSearchCategory.COMMON_NAME, JHSearchCategory.LOCATION, JHSearchCategory.DATE,
                        JHSearchCategory.NUM_ILLUS, JHSearchCategory.NUM_PAGES, JHSearchCategory.ORIGIN, JHSearchCategory.TYPE,
                        JHSearchCategory.TRANSCRIPTION});
        
        searchfields.put("aorcollection",
                new JHSearchField[] {
                        JHSearchField.MARGINALIA,
                        JHSearchField.SYMBOL,
                        JHSearchField.UNDERLINE,
                        JHSearchField.MARK,
                        JHSearchField.BOOK,
                        JHSearchField.PEOPLE,
                        JHSearchField.PLACE,
                        JHSearchField.LANGUAGE,
                        JHSearchField.MARGINALIA_LANGUAGE,
                        JHSearchField.NUMERAL,
                        JHSearchField.DRAWING,
                        JHSearchField.ERRATA,            
                        JHSearchField.EMPHASIS,
                        JHSearchField.CROSS_REFERENCE,
                        JHSearchField.METHOD,
                        JHSearchField.CALCULATION,
                        JHSearchField.GRAPH,
                        JHSearchField.TABLE,
                        JHSearchField.HAND,
                        JHSearchField.ANNOTATOR
                });
        searchcategories.put("aorcollection",
                new JHSearchCategory[] {JHSearchCategory.AUTHOR, JHSearchCategory.COMMON_NAME, JHSearchCategory.DATE,
                        JHSearchCategory.LOCATION, JHSearchCategory.NUM_PAGES, JHSearchCategory.ORIGIN});
        
        searchfields.put("top",
                new JHSearchField[] {
                        JHSearchField.DESCRIPTION,
                        JHSearchField.TITLE,
                        JHSearchField.PEOPLE,
                        JHSearchField.PLACE,
                        JHSearchField.REPO,
                        JHSearchField.TEXT});
        searchcategories.put("top",
                new JHSearchCategory[] {JHSearchCategory.AUTHOR, JHSearchCategory.COMMON_NAME, JHSearchCategory.LOCATION,
                        JHSearchCategory.DATE, JHSearchCategory.REPOSITORY});

        searchfields.put("dlmm",
                new JHSearchField[] {
                        JHSearchField.DESCRIPTION,
                        JHSearchField.TITLE,
                        JHSearchField.PEOPLE,
                        JHSearchField.PLACE,
                        JHSearchField.REPO,
                        JHSearchField.TRANSCRIPTION});
        searchcategories.put("dlmm",
                new JHSearchCategory[]{JHSearchCategory.AUTHOR, JHSearchCategory.COMMON_NAME, JHSearchCategory.LOCATION,
                        JHSearchCategory.NUM_ILLUS, JHSearchCategory.NUM_PAGES, JHSearchCategory.ORIGIN,
                        JHSearchCategory.TYPE, JHSearchCategory.TRANSCRIPTION});
        }
    
    /**
     * @param path
     * @param formatter
     * @throws IOException
     */
    public LuceneJHSearchService(Path path, IIIFPresentationRequestFormatter formatter) throws IOException {
        super(path, new JHSearchLuceneMapper(formatter));

        this.serializer = new JHSearchSerializer();
        this.formatter = formatter;
    }

    @Override
    public void handle_request(PresentationRequest req, String query, int offset, int max, String sort_order, String categories, OutputStream os)
            throws IOException {

        SearchOptions opts = new SearchOptions();
        opts.setMatchCount(max);
        opts.setOffset(offset);
        
        if (sort_order != null) {
        	try {
        		opts.setSortOrder(SortOrder.valueOf(sort_order.toUpperCase()));
        	} catch (IllegalArgumentException e) {
        		throw new IllegalArgumentException("Unknown sort order: " + sort_order); 
        	}
        }
        
        Query search_query;
        
        try {
            search_query = QueryParser.parseQuery(query);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Query error: " + e.getMessage()); 
        }
        
        if (categories != null) {
            try {
                opts.setCategories(QueryParser.parseTermList(categories));
            } catch (ParseException e) {
                throw new IllegalArgumentException("Category error: " + e.getMessage()); 
            }
        }
        
        Query restrict_query = null;
        
        // Only supports collection and manifest restrictions
        
        String req_url = formatter.format(req);

        // Restrict to a specific collection if the collection is not "top"
        // TODO We should load the archive collection to check for child/parent relationships in order to properly generate the restriction query
        if (req.getType() == PresentationRequestType.COLLECTION) {
            String name = req.getIdentifier()[0]; 
                    
            if ("dlmm".equals(name)) {
                String rose_url = formatter.format(
                        new PresentationRequest(PresentationRequestType.COLLECTION, "rosecollection"));
                String pizan_url = formatter.format(
                        new PresentationRequest(PresentationRequestType.COLLECTION, "pizancollection"));
                restrict_query = new Query(
                        QueryOperation.OR,
                        new Query(JHSearchField.COLLECTION_ID, rose_url),
                        new Query(JHSearchField.COLLECTION_ID, pizan_url)
                );
            } else if (!"top".equals(name)) {
                restrict_query = new Query(JHSearchField.COLLECTION_ID, req_url);
            }
        } else if (req.getType() == PresentationRequestType.MANIFEST) {
            restrict_query = new Query(JHSearchField.MANIFEST_ID, req_url);
        }
        
        if (restrict_query != null) {
            search_query = new Query(QueryOperation.AND, search_query, restrict_query);
        }
        
        SearchResult result = search(search_query, opts);

        // TODO Put URL generation elsewhere
        StringBuilder url = new StringBuilder(req_url);
        url.append(RESOURCE_PATH).append('?')
                .append(QUERY_PARAM).append('=').append(URLEncoder.encode(query, "UTF-8"))
                .append('&').append(MAX_MATCHES_PARAM).append('=').append(opts.getMatchCount());

        if (opts.getSortOrder() != null) {
            url.append('&').append(SORT_ORDER_PARAM).append('=').append(URLEncoder.encode(opts.getSortOrder().name().toLowerCase(), "UTF-8"));
        }

        url.append('&').append(OFFSET_PARAM).append('=').append(opts.getOffset());

        if (categories != null && !categories.equals("")) {
            url.append('&').append(CATEGORIES).append('=').append(URLEncoder.encode(categories, "UTF-8"));
        }
        
        serializer.write(url.toString(), query, result, os);
    }


    @Override
    public void update(Store store) throws IOException {
        for (String col: store.listBookCollections()) {
            update(store, col);
        }
    }

    @Override
    public void handle_info_request(PresentationRequest req, OutputStream os) throws IOException {
        String name = null;
        
        if (req.getIdentifier().length > 0) {
            name = req.getIdentifier()[0];
        }
        
        JHSearchField[] fields = searchfields.get(name);
        JHSearchCategory[] categories = searchcategories.get(name);
        
        if (fields == null) {
            fields = new JHSearchField[]{};
        }
        
        if (categories == null) {
            categories = new JHSearchCategory[0];
        }

        serializer.write(fields, categories, os);
    }

	@Override
	public boolean has_content() throws IOException {
		return !isEmpty();
	}
}
