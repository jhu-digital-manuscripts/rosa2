package rosa.iiif.presentation.core;

import java.io.IOException;
import java.io.OutputStream;

import rosa.iiif.presentation.model.PresentationRequest;

/**
 * Represents the IIIF Presentation API.
 */
public interface IIIFService {

    /**
     * Serialize the IIIF Presentation API object referenced by the given URI as
     * JSON-LD.
     * 
     * @param uri
     * @param os
     * @return whether or not the URI exists
     */
    boolean handle_request(String uri, OutputStream os) throws IOException;

    /**
     * Serialize the IIIF Presentation API object referenced by the given
     * request.
     * 
     * This method is used preferentially
     * 
     * @param req
     * @param os
     * @return whether or not the URI exists
     */
    boolean handle_request(PresentationRequest req, OutputStream os) throws IOException;
}
