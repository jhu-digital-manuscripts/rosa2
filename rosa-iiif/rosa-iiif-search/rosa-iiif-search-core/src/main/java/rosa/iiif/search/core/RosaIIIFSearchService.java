package rosa.iiif.search.core;

import rosa.archive.core.Store;
import rosa.iiif.search.model.IIIFSearchRequest;
import rosa.iiif.search.model.IIIFSearchResult;
import rosa.search.core.SearchService;
import rosa.search.model.SearchOptions;

import java.io.IOException;
import java.util.logging.Logger;

public class RosaIIIFSearchService implements IIIFSearchService {
    private static final Logger logger = Logger.getLogger(RosaIIIFSearchService.class.toString());

    private final Store store;
    private final SearchService searchService;
    private final IIIFLuceneSearchAdapter adapter;

    private final String collectionId;

    public RosaIIIFSearchService(Store store, SearchService searchService, IIIFLuceneSearchAdapter adapter,
                                 String collectionId) {
        logger.info("Creating rosa IIIF search service. [" + collectionId + "]");
        this.searchService = searchService;
        this.adapter = adapter;
        this.store = store;
        this.collectionId = collectionId;
    }

    @Override
    public IIIFSearchResult search(IIIFSearchRequest request) throws IOException {
        logger.info("Searching: " + request.toString());
        SearchOptions options = new SearchOptions();
        options.setOffset(request.page);
        options.setMatchCount(Integer.MAX_VALUE);

        return adapter.luceneResultToIIIF(
                searchService.search(adapter.iiifToLuceneQuery(request), options)
        );
    }

    @Override
    public void update() throws IOException {
        searchService.update(store, collectionId);
    }

    @Override
    public void shutdown() throws IOException {
        searchService.shutdown();
    }


}
