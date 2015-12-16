package rosa.iiif.search.core;

import rosa.archive.core.Store;
import rosa.iiif.search.model.IIIFSearchRequest;
import rosa.iiif.search.model.IIIFSearchResult;
import rosa.iiif.search.model.Rectangle;
import rosa.search.core.SearchService;
import rosa.search.model.SearchOptions;

import java.io.IOException;
import java.util.logging.Logger;

public class RosaIIIFSearchService implements IIIFSearchService {
    private static final Logger logger = Logger.getLogger(RosaIIIFSearchService.class.toString());

    private final Store store;
    private final SearchService searchService;
    private final IIIFLuceneSearchAdapter adapter;
    private final IIIFSearchRequestFormatter requestFormatter;

    private final String collectionId;

    public RosaIIIFSearchService(Store store, SearchService searchService, IIIFLuceneSearchAdapter adapter,
                                 String collectionId, IIIFSearchRequestFormatter requestFormatter) {
        logger.info("Creating rosa IIIF search service. [" + collectionId + "]");
        this.searchService = searchService;
        this.adapter = adapter;
        this.store = store;
        this.collectionId = collectionId;
        this.requestFormatter = requestFormatter;
    }

    @Override
    public IIIFSearchResult search(IIIFSearchRequest request) throws IOException {
        logger.info("Searching: " + request.toString());
        SearchOptions options = new SearchOptions();
        options.setOffset(request.page);
        options.setMatchCount(Integer.MAX_VALUE);

        IIIFSearchResult result = adapter.luceneResultToIIIF(
                searchService.search(adapter.iiifToLuceneQuery(request), options)
        );
        result.setId(requestFormatter.format(request));

        // TODO do fields: prev, next, first, last where applicable

        return result;
    }

    @Override
    public void update() throws IOException {
        searchService.update(store, collectionId);
    }

    @Override
    public void shutdown() throws IOException {
        searchService.shutdown();
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


}
