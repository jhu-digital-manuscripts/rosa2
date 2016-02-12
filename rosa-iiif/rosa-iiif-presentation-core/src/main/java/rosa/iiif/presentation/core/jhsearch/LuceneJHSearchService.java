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

public class LuceneJHSearchService extends LuceneSearchService implements JHSearchService {
    private final JHSearchSerializer serializer;
    private final IIIFPresentationRequestFormatter formatter;
    
    public LuceneJHSearchService(Path path, IIIFPresentationRequestFormatter formatter) throws IOException {
        super(path, new JHSearchLuceneMapper(formatter));

        this.serializer = new JHSearchSerializer();
        this.formatter = formatter;
    }

    @Override
    public void handle_request(PresentationRequest req, String query, int offset, int max, OutputStream os)
            throws IOException {

        SearchOptions opts = new SearchOptions();
        opts.setMatchCount(max);
        opts.setOffset(offset);
        
        handle_request(req, query, opts, os);
    }

    @Override
    public void handle_request(PresentationRequest req, String query, String resume, int max, OutputStream os)
            throws IOException {

        SearchOptions opts = new SearchOptions();
        opts.setMatchCount(max);
        opts.setResumeToken(resume);
        
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
        
        if (req.getType() == PresentationRequestType.COLLECTION) {
            restrict_query = new Query(JHSearchFields.COLLECTION_ID, req_url);
        } else if (req.getType() == PresentationRequestType.MANIFEST) {
            restrict_query = new Query(JHSearchFields.MANIFEST_ID, req_url);
        }
        
        if (restrict_query != null) {
            search_query = new Query(QueryOperation.AND, search_query, restrict_query);
        }
        
        SearchResult result = search(search_query, opts);

        String url = req_url + RESOURCE_PATH + "?q=" + URLEncoder.encode(query, "UTF-8") + "&m=" + opts.getMatchCount();
        
        if (opts.getResumeToken() == null) {
            url += "&o=" + opts.getOffset();
        } else {
            url += "&r=" + URLEncoder.encode(opts.getResumeToken(), "UTF-8");
        }
        
        serializer.write(url, query, result, os);
    }


    @Override
    public void update(Store store) throws IOException {
        for (String col: store.listBookCollections()) {
            update(store, col);
        }
    }
}
