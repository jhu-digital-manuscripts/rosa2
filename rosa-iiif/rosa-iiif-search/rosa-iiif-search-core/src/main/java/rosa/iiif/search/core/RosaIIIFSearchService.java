package rosa.iiif.search.core;

import rosa.iiif.search.model.IIIFSearchRequest;
import rosa.iiif.search.model.IIIFSearchResult;
import rosa.search.core.SearchService;
import rosa.search.model.SearchOptions;

import java.io.IOException;

public class RosaIIIFSearchService implements IIIFSearchService {

    private final SearchService searchService;
    private final IIIFLuceneSearchAdapter adapter;

    public RosaIIIFSearchService(SearchService searchService, IIIFLuceneSearchAdapter adapter) {
        this.searchService = searchService;
        this.adapter = adapter;
    }

    @Override
    public IIIFSearchResult search(IIIFSearchRequest request) throws IOException {
        SearchOptions options = new SearchOptions();
        options.setOffset(request.page);
        options.setMatchCount(Integer.MAX_VALUE);

        return adapter.luceneResultToIIIF(
                searchService.search(adapter.iiifToLuceneQuery(request), options)
        );
    }
}
