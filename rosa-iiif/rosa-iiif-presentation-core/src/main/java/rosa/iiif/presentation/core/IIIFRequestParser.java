package rosa.iiif.presentation.core;

import rosa.iiif.image.core.UriUtil;
import rosa.iiif.presentation.model.PresentationRequest;
import rosa.iiif.presentation.model.PresentationRequestType;

/**
 * Parse a IIIF Presentation API according to the recommended URI patterns.
 */
public class IIIFRequestParser {
    private final String path_prefix;

    /**
     * @param path_prefix
     *            must not be decoded
     */
    public IIIFRequestParser(String path_prefix) {
        this.path_prefix = path_prefix;
    }

    private String get_relative_path(String path) {
        if (path_prefix != null && path.startsWith(path_prefix)) {
            path = path.substring(path_prefix.length());
        }

        if (path.length() > 0 && path.charAt(0) == '/') {
            path = path.substring(1);
        }

        return path;
    }

    private String[] split_path(String path) {
        String[] parts = get_relative_path(path).split("/");

        for (int i = 0; i < parts.length; i++) {
            parts[i] = UriUtil.decodePathSegment(parts[i]);
        }

        return parts;
    }

    private PresentationRequestType getType(String keyword) {
        for (PresentationRequestType type : PresentationRequestType.values()) {
            if (type.getKeyword().equals(keyword)) {
                return type;
            }
        }

        return null;
    }

    /**
     * @param path
     * @return null on failure
     */
    public PresentationRequest parsePresentationRequest(String path) {
        path = get_relative_path(path);

        if (!UriUtil.isValidEncodedPath(path)) {
            return null;
        }

        PresentationRequest req = new PresentationRequest();

        String[] parts = split_path(path);

        if (parts.length == 2) {
            if (parts[0].equals(PresentationRequestType.COLLECTION.getKeyword())) {
                req.setType(PresentationRequestType.COLLECTION);
                req.setName(parts[1]);
            } else if (parts[0].equals(PresentationRequestType.MANIFEST.getKeyword())) {
                req.setId(parts[0]);
                req.setType(PresentationRequestType.MANIFEST);
            } else {
                return null;
            }
        } else if (parts.length == 3) {
            req.setId(parts[0]);
            req.setType(getType(parts[1]));
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
