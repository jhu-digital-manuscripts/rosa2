package rosa.iiif.presentation.core;

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

        if (parts.length == 2) {
            if (parts[0].equals(PresentationRequestType.COLLECTION.getKeyword())) {
                req.setType(PresentationRequestType.COLLECTION);
                req.setName(parts[1]);
            } else if (parts[1].equals(PresentationRequestType.MANIFEST.getKeyword())) {
                req.setId(parts[0]);
                req.setType(PresentationRequestType.MANIFEST);
            } else {
                return null;
            }
        } else if (parts.length == 3) {
            PresentationRequestType type = parse_type(parts[1]);
            
            if (type == null || type == PresentationRequestType.MANIFEST || type == PresentationRequestType.COLLECTION) {
                return null;
            }
            
            req.setId(parts[0]);
            req.setType(type);
            req.setName(parts[2]);            
        } else {
            return null;
        }

        if (req.getType() == null) {
            return null;
        }

        return req;
    }
}
