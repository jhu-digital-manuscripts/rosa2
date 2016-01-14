package rosa.iiif.presentation.core.search;

import rosa.iiif.presentation.model.search.IIIFSearchRequest;
import rosa.iiif.presentation.model.search.IIIFSearchResult;

import java.io.IOException;

public interface IIIFSearchService {
    IIIFSearchResult search(IIIFSearchRequest request) throws IOException;
    void update() throws IOException;
    void shutdown() throws IOException;
}
