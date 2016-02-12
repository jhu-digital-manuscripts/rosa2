package rosa.iiif.presentation.core;

import java.io.IOException;
import java.io.OutputStream;

import rosa.iiif.presentation.model.PresentationRequest;

/**
 * Represents the IIIF Presentation API.
 */
public interface IIIFPresentationService {

    /**
     * Serialize the IIIF Presentation API object referenced by the given URI as
     * JSON-LD.
     * 
     * @param uri incoming URI to process
     * @param os output stream to write to
     * @return whether or not the URI exists
     * @throws java.io.IOException
     */
    boolean handle_request(String uri, OutputStream os) throws IOException;
        
    /**
     * Serialize the IIIF Presentation API object referenced by the given
     * request.
     * 
     * This method is used preferentially
     * 
     * @param req presentation request, that contains information about the incoming request
     * @param os output stream to write response
     * @return whether or not the URI exists
     * @throws java.io.IOException
     */
    boolean handle_request(PresentationRequest req, OutputStream os) throws IOException;
}
