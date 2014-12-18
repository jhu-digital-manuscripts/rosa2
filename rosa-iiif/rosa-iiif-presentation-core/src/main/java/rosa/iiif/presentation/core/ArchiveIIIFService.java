package rosa.iiif.presentation.core;

import java.io.IOException;
import java.io.OutputStream;

import rosa.iiif.presentation.model.PresentationRequest;

/**
 * An implementation of the IIIF Presentation API that transforms objects from
 * the archive model to the presentation model.
 * 
 * TODO Describe id/uri mapping
 */
public class ArchiveIIIFService implements IIIFService {

    @Override
    public boolean handle_request(String uri, OutputStream os) throws IOException {
        return false;
    }

    @Override
    public boolean handle_request(PresentationRequest req, OutputStream os) throws IOException {
        return false;
    }
}
