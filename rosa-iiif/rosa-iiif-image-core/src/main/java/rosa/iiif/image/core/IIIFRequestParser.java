package rosa.iiif.image.core;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import rosa.iiif.image.model.ImageFormat;
import rosa.iiif.image.model.ImageRequest;
import rosa.iiif.image.model.InfoFormat;
import rosa.iiif.image.model.InfoRequest;
import rosa.iiif.image.model.Quality;
import rosa.iiif.image.model.Region;
import rosa.iiif.image.model.RegionType;
import rosa.iiif.image.model.RequestType;
import rosa.iiif.image.model.Rotation;
import rosa.iiif.image.model.Size;
import rosa.iiif.image.model.SizeType;

/**
 * Parse an raw URL path info a IIIF request. If a path_prefix is given, it is
 * stripped from paths before parsing.
 */
public class IIIFRequestParser {
    private final String path_prefix;

    public IIIFRequestParser() {
        this(null);
    }

    /**
     * @param path_prefix must not be decoded
     */
    public IIIFRequestParser(String path_prefix) {
        this.path_prefix = path_prefix;
    }

    /**
     * Determine type of a IIIF request.
     * 
     * @param path must not be decoded
     * @return type of the request.
     */
    public RequestType determineRequestType(String path) {
        if (path.endsWith("/info.json")) {
            return RequestType.INFO;
        } else {
            return RequestType.IMAGE;
        }
    }

    private String[] split(String path) {
        if (path_prefix != null && path.startsWith(path_prefix)) {
            path = path.substring(path_prefix.length());
        }

        if (path.length() > 0 && path.charAt(0) == '/') {
            path = path.substring(1);
        }

        String[] parts = path.split("/");

        for (int i = 0; i < parts.length; i++) {
            try {
                // TODO This is wrong, must use real uri decoder 
                
                parts[i] = URLDecoder.decode(parts[i], "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        return parts;
    }

    /**
     * Parse a IIIF Image info request.
     * 
     * @param path must not be decoded
     * @return image info request
     * @throws IIIFException
     */
    public InfoRequest parseImageInfoRequest(String path) throws IIIFException {
        String[] parts = split(path);

        if (parts.length != 2) {
            throw new IIIFException("Malformed info request: " + path);
        }

        InfoRequest req = new InfoRequest();
        req.setImageId(parts[0]);

        if (parts[1].equals("info.json")) {
            req.setFormat(InfoFormat.JSON);
        } else {
            throw new IIIFException("Format not available: " + parts[1], "format");
        }

        return req;
    }

    /**
     * Parse a IIIF Image request.
     * 
     * @param path must not be decoded
     * @return image request
     * @throws IIIFException
     */
    public ImageRequest parseImageRequest(String path) throws IIIFException {
        String[] parts = split(path);

        if (parts.length != 5) {
            throw new IIIFException("Malformed image request: " + path);
        }

        ImageRequest req = new ImageRequest();
        req.setImageId(parts[0]);

        req.setRegion(parseRegion(parts[1]));
        req.setSize(parseSize(parts[2]));
        req.setRotation(parseRotation(parts[3]));

        String[] last = parts[4].split("\\.");

        if (last.length != 2) {
            throw new IIIFException("Malformed image request: " + path);
        }

        req.setQuality(parseQuality(last[0]));        
        req.setFormat(parseImageFormat(last[1]));

        return req;
    }

    private ImageFormat parseImageFormat(String file_ext) throws IIIFException {
        for (ImageFormat fmt : ImageFormat.values()) {
            if (fmt.getFileExtension().equals(file_ext)) {
                return fmt;
            }
        }

        throw new IIIFException("Unknown image format: " + file_ext, "format");
    }

    private Size parseSize(String s) throws IIIFException {
        Size size = new Size();

        try {

            if (s.equals("full")) {
                size.setSizeType(SizeType.FULL);
                return size;
            }

            if (s.endsWith(",")) {
                s = s.substring(0, s.length() - 1);
                size.setSizeType(SizeType.EXACT_WIDTH);

                size.setWidth(Integer.parseInt(s));
                return size;
            }

            if (s.startsWith(",")) {
                s = s.substring(1);
                size.setSizeType(SizeType.EXACT_HEIGHT);

                size.setHeight(Integer.parseInt(s));
                return size;
            }

            if (s.startsWith("pct:")) {
                s = s.substring(4);

                size.setSizeType(SizeType.PERCENTAGE);

                size.setPercentage(Double.parseDouble(s));

                return size;
            }

            if (s.startsWith("!")) {
                s = s.substring(1);
                size.setSizeType(SizeType.BEST_FIT);
            } else {
                size.setSizeType(SizeType.EXACT);
            }

            String[] parts = s.split(",");

            if (parts.length != 2) {
                throw new IIIFException("Malformed size", "size");
            }

            size.setWidth(Integer.parseInt(parts[0]));
            size.setHeight(Integer.parseInt(parts[1]));

            return size;
        } catch (NumberFormatException e) {
            throw new IIIFException("Malformed number: " + e.getMessage(), "size");
        }
    }

    private Quality parseQuality(String s) throws IIIFException {
        for (Quality q : Quality.values()) {
            if (q.getKeyword().equals(s)) {
                return q;
            }
        }

        throw new IIIFException("Unsupported quality: " + s, "quality");
    }

    private Rotation parseRotation(String s) throws IIIFException {
        Rotation rot = new Rotation();

        boolean mirrored = !s.isEmpty() && s.charAt(0) == '!';

        if (mirrored) {
            s = s.substring(1);
        }

        rot.setMirrored(mirrored);

        double angle;

        try {
            angle = Double.parseDouble(s);
        } catch (NumberFormatException e) {
            throw new IIIFException("Malformed rotation: " + e.getMessage(), "rotation");
        }

        if (angle < 0.0 || angle > 360.0) {
            throw new IIIFException("Invalid rotation: " + angle, "rotation");
        }

        rot.setAngle(angle);

        return rot;
    }

    private Region parseRegion(String s) throws IIIFException {
        Region region = new Region();

        try {
            if (s.equals("full")) {
                region.setRegionType(RegionType.FULL);
                return region;
            }

            String[] parts = s.split(",");

            if (parts.length != 4) {
                throw new IIIFException("Malformed region", "region");
            }

            if (s.startsWith("pct:")) {
                parts[0] = parts[0].substring(4);

                region.setRegionType(RegionType.PERCENTAGE);
                region.setPercentageX(Double.parseDouble(parts[0]));
                region.setPercentageY(Double.parseDouble(parts[1]));
                region.setPercentageWidth(Double.parseDouble(parts[2]));
                region.setPercentageHeight(Double.parseDouble(parts[3]));
            } else {
                region.setRegionType(RegionType.ABSOLUTE);
                region.setX(Integer.parseInt(parts[0]));
                region.setY(Integer.parseInt(parts[1]));
                region.setWidth(Integer.parseInt(parts[2]));
                region.setHeight(Integer.parseInt(parts[3]));
            }

            return region;
        } catch (NumberFormatException e) {
            throw new IIIFException("Malformed number: " + e.getMessage(), "region");
        }
    }
}
