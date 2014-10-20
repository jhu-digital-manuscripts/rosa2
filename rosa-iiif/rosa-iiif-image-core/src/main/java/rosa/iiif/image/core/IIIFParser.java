package rosa.iiif.image.core;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import rosa.iiif.image.model.ImageFormat;
import rosa.iiif.image.model.ImageRequest;
import rosa.iiif.image.model.InfoFormat;
import rosa.iiif.image.model.InfoRequest;
import rosa.iiif.image.model.Quality;
import rosa.iiif.image.model.Region;
import rosa.iiif.image.model.RegionType;
import rosa.iiif.image.model.RequestType;
import rosa.iiif.image.model.Size;
import rosa.iiif.image.model.SizeType;

/**
 * Parse a URL path info a IIIF request. If a path_prefix is given, it is
 * stripped from paths before parsing.
 */
public class IIIFParser {
    private final String path_prefix;
    private final Map<String, String> aliases; // alias -> fsi image id

    public IIIFParser() {
        this(null);
    }

    public IIIFParser(String path_prefix) {
        this.path_prefix = path_prefix;
        this.aliases = new HashMap<String, String>();
    }

    public RequestType determineRequestType(String path) {
        if (path.endsWith("/info.xml") || path.endsWith("/info.json")) {
            return RequestType.INFO;
        } else {
            return RequestType.IMAGE;
        }
    }

    public Map<String, String> getImageAliases() {
        return aliases;
    }

    private String[] parse(String path) {
        if (path_prefix != null && path.startsWith(path_prefix)) {
            path = path.substring(path_prefix.length());
        }

        if (path.length() > 0 && path.charAt(0) == '/') {
            path = path.substring(1);
        }

        String[] parts = path.split("/");

        for (int i = 0; i < parts.length; i++) {
            try {
                parts[i] = URLDecoder.decode(parts[i], "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        return parts;
    }

    public InfoRequest parseImageInfoRequest(String path) throws IIIFException {
        String[] parts = parse(path);

        if (parts.length != 2) {
            throw new IIIFException("Malformed info request: " + path);
        }

        InfoRequest req = new InfoRequest();

        if (aliases.containsKey(parts[0])) {
            req.setImageId(aliases.get(parts[0]));
        } else {
            req.setImageId(parts[0]);
        }

        if (parts[1].equals("info.json")) {
            req.setFormat(InfoFormat.JSON);
        } else {
            throw new IIIFException("Format not available: " + parts[1], "format");
        }

        return req;
    }

    public ImageRequest parseImageRequest(String path) throws IIIFException {
        String[] parts = parse(path);

        if (parts.length != 5) {
            throw new IIIFException("Malformed image request: " + path);
        }

        ImageRequest req = new ImageRequest();

        if (aliases.containsKey(parts[0])) {
            req.setImageId(aliases.get(parts[0]));
        } else {
            req.setImageId(parts[0]);
        }

        req.setRegion(parseRegion(parts[1]));
        req.setSize(parseSize(parts[2]));

        double rotation;

        try {
            rotation = Double.parseDouble(parts[3]);
        } catch (NumberFormatException e) {
            throw new IIIFException("Malformed rotation: " + e.getMessage(), "rotation");
        }

        if (rotation < 0 || rotation > 360) {
            throw new IIIFException("Invalid rotation: " + rotation, "rotation");
        }
        
        // TODO fix
        //req.setRotation(rotation);

        String[] last = parts[4].split("\\.");

        if (last.length != 1 && last.length != 2) {
            throw new IIIFException("Malformed image request");
        }

        req.setQuality(parseQuality(last[0]));

        if (last.length == 2) {
            req.setFormat(parseImageFormat(last[1]));
        }

        return req;
    }

    private ImageFormat parseImageFormat(String s) throws IIIFException {
        // TODO fix
        try {
            return ImageFormat.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IIIFException("Malformed image format", "format");
        }
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
        try {
            return Quality.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IIIFException("Malformed quality", "quality");
        }
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
