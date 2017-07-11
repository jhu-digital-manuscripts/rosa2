package rosa.iiif.presentation.core.jhsearch;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.file.Path;

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
    
    public LuceneJHSearchService(Path path, IIIFPresentationRequestFormatter formatter) throws IOException {
        super(path, new JHSearchLuceneMapper(formatter));

        this.serializer = new JHSearchSerializer();
        this.formatter = formatter;
    }

    @Override
    public void handle_request(PresentationRequest req, String query, int offset, int max, String sort_order, OutputStream os)
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
        
        handle_request(req, query, opts, os);
    }

    private void handle_request(PresentationRequest req, String query, SearchOptions opts, OutputStream os) throws IOException {
        Query search_query;
        
        try {
            search_query = QueryParser.parseQuery(query);
        } catch (ParseException e) {
        	throw new IllegalArgumentException("Query error: " + e.getMessage()); 
        }
        
        Query restrict_query = null;
        
        // Only supports collection and manifest restrictions
        
        String req_url = formatter.format(req);

        // Restrict to a specific collection if the collection is not "top"
        if (req.getType() == PresentationRequestType.COLLECTION && !"top".equals(req.getName())) {
            restrict_query = new Query(JHSearchField.COLLECTION_ID, req_url);
        } else if (req.getType() == PresentationRequestType.MANIFEST) {
            restrict_query = new Query(JHSearchField.MANIFEST_ID, req_url);
        }
        
        if (restrict_query != null) {
            search_query = new Query(QueryOperation.AND, search_query, restrict_query);
        }
        
        SearchResult result = search(search_query, opts);

        // TODO Put URL generation elsewhere
        
        String url = req_url + RESOURCE_PATH + "?" + JHSearchService.QUERY_PARAM + "=" + URLEncoder.encode(query, "UTF-8") + "&" + JHSearchService.MAX_MATCHES_PARAM + "=" + opts.getMatchCount();
        
        if (opts.getSortOrder() != null) {
            url += "&" + JHSearchService.SORT_ORDER_PARAM + "=" + URLEncoder.encode(opts.getSortOrder().name().toLowerCase(), "UTF-8");
        }
        
        url += "&" + JHSearchService.OFFSET_PARAM + "=" + opts.getOffset();
        
        serializer.write(url, query, result, os);
    }


    @Override
    public void update(Store store) throws IOException {
        for (String col: store.listBookCollections()) {
            update(store, col);
        }
    }

    // TODO Hack to return search fields based on collection.
    // TODO Refactor to make search service per collection?
    
    private static final JHSearchField[] ROSE_PIZAN_FIELDS = {
            JHSearchField.DESCRIPTION,
            JHSearchField.TRANSCRIPTION,
            JHSearchField.ILLUSTRATION, 
            JHSearchField.TITLE,
            JHSearchField.REPO
    };
    
    private static final JHSearchField[] AOR_FIELDS = {
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
    };

    private static final JHSearchField[] SHARED_FIELDS = {
            JHSearchField.DESCRIPTION,
            JHSearchField.TITLE,
            JHSearchField.PEOPLE,
            JHSearchField.PLACE,
            JHSearchField.REPO,
            JHSearchField.TEXT
    };

    private static final JHSearchField[] HAMLET_FIELDS = {
            JHSearchField.REPLACEMENTS,
            JHSearchField.INSERTIONS,
            JHSearchField.DELETIONS,
            JHSearchField.ERRATA,
            JHSearchField.MARGINALIA,
            JHSearchField.SYMBOL,
            JHSearchField.UNDERLINE,
            JHSearchField.EMPHASIS,
            JHSearchField.MARK,
            JHSearchField.BOOK,
            JHSearchField.PEOPLE,
            JHSearchField.PLACE,
            JHSearchField.METHOD
    };
    
    @Override
    public void handle_info_request(PresentationRequest req, OutputStream os) throws IOException {
        JHSearchField[] fields;
        String identifier = req.getType() == PresentationRequestType.COLLECTION ? req.getName() : req.getId();
        
        if (identifier.contains("rose") || identifier.contains("pizan")) {
            fields = ROSE_PIZAN_FIELDS;
        } else if (identifier.contains("aor")) {
            fields = AOR_FIELDS;
        } else if (identifier.contains("hamlet")) {
            fields = HAMLET_FIELDS;
        } else {
            fields = SHARED_FIELDS;
        }
        
        serializer.write(fields, os);    
    }

	@Override
	public boolean has_content() throws IOException {
		return !isEmpty();
	}
}
