package rosa.iiif.presentation.core;

import java.util.Arrays;

import rosa.iiif.image.core.UriUtil;
import rosa.iiif.presentation.model.PresentationRequest;
import rosa.iiif.presentation.model.PresentationRequestType;

/**
 * Parse a IIIF Presentation API according to the recommended URI patterns.
 */
public class IIIFPresentationRequestParser {
    private PresentationRequestType parse_type(String keyword) {
        for (PresentationRequestType type : PresentationRequestType.values()) {
            if (type.getKeyword().equals(keyword)) {
                return type;
            }
        }

        return null;
    }

    /**
     * @param path must be correctly encoded 
     * @return null on failure
     */
    public PresentationRequest parsePresentationRequest(String path) {
        if (!UriUtil.isValidEncodedPath(path)) {
            return null;
        }

        PresentationRequest req = new PresentationRequest();

        String[] parts = UriUtil.decodePathSegments(path);

        if (parts.length < 2) {
            return null;
        }
        
        req.setIdentifier(Arrays.copyOf(parts, parts.length - 1));
        req.setType(parse_type(parts[parts.length - 1]));
            
        if (req.getType() == null) {
            return null;
        }
        
        return req;
    }
}
