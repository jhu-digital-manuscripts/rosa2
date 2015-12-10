package rosa.iiif.search.core;

import rosa.iiif.search.model.IIIFSearchRequest;
import rosa.iiif.search.model.IIIFSearchResult;

import java.io.IOException;

public interface IIIFSearchService {
    IIIFSearchResult search(IIIFSearchRequest request) throws IOException;
}
