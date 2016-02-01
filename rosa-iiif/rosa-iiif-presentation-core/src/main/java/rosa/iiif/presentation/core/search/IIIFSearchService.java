package rosa.iiif.presentation.core.search;

import java.io.IOException;

import rosa.archive.core.Store;
import rosa.iiif.presentation.model.search.IIIFSearchRequest;
import rosa.iiif.presentation.model.search.IIIFSearchResult;

// TODO doc
public interface IIIFSearchService {
    IIIFSearchResult search(IIIFSearchRequest request) throws IOException;
    
    void update(Store store) throws IOException;
    
    void shutdown() throws IOException;
}
