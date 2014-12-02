package rosa.iiif.image.core;

import rosa.iiif.image.model.ImageRequest;
import rosa.iiif.image.model.InfoRequest;
import rosa.iiif.image.model.Region;
import rosa.iiif.image.model.Rotation;
import rosa.iiif.image.model.Size;

/**
 * Format IIIF requests into URIs.
 */
public class IIIFRequestFormatter {
    private final String scheme;
    private final String host;
    private final String prefix;
    private final int port;

    /**
     * @param scheme
     *            http or https
     * @param host
     * @param port
     *            -1 for default port
     * @param prefix
     *            must be encoded, start with '/', and not end with '/'.
     */
    public IIIFRequestFormatter(String scheme, String host, int port, String prefix) {
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.prefix = prefix;
    }

    /**
     * @param req
     * @return URI for request
     */
    public String format(InfoRequest req) {
        return base() + UriUtil.encodePathSegment(req.getImageId()) + "/info.json";
    }

    public String format(ImageRequest req) {
        return base()
                + UriUtil.encodePathSegments(req.getImageId(), format(req.getRegion()), format(req.getSize()),
                        format(req.getRotation()), req.getQuality().getKeyword() + "."
                                + req.getFormat().getFileExtension());
    }

    private String base() {
        return scheme + "://" + host + (port == -1 ? "" : ":" + port) + prefix + "/";
    }

    private String format(Region reg) {
        switch (reg.getRegionType()) {
        case ABSOLUTE:
            return reg.getX() + "," + reg.getY() + "," + reg.getWidth() + "," + reg.getHeight();
        case FULL:
            return "full";
        case PERCENTAGE:
            return "pct:" + reg.getPercentageX() + "," + reg.getPercentageY() + "," + reg.getPercentageWidth() + ","
                    + reg.getPercentageHeight();
        default:
            throw new AssertionError();
        }
    }

    private String format(Size size) {
        switch (size.getSizeType()) {
        case BEST_FIT:
            return "!" + size.getWidth() + "," + size.getHeight();
        case EXACT:
            return size.getWidth() + "," + size.getHeight();
        case EXACT_HEIGHT:
            return "," + size.getHeight();
        case EXACT_WIDTH:
            return size.getWidth() + ",";
        case FULL:
            return "full";
        case PERCENTAGE:
            return "pct:" + size.getPercentage();
        default:
            throw new AssertionError();
        }
    }

    private String format(Rotation rotation) {
        // Formatted angle should be integer if possible and not have trailing
        // zeros. If < 0, then should start with 0.

        String angle = String.valueOf(rotation.getAngle());

        if (angle.endsWith(".0")) {
            angle = angle.substring(0, angle.length() - 2);
        }

        return (rotation.isMirrored() ? "!" : "") + angle;
    }
}
