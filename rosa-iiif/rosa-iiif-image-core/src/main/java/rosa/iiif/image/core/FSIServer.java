package rosa.iiif.image.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import rosa.iiif.image.model.ComplianceLevel;
import rosa.iiif.image.model.ImageFormat;
import rosa.iiif.image.model.ImageInfo;
import rosa.iiif.image.model.ImageRequest;
import rosa.iiif.image.model.ImageServerProfile;
import rosa.iiif.image.model.Quality;
import rosa.iiif.image.model.Region;
import rosa.iiif.image.model.RegionType;
import rosa.iiif.image.model.Rotation;
import rosa.iiif.image.model.Size;
import rosa.iiif.image.model.SizeType;

/**
 * Use FSI server http api to fullfull requests.
 * 
 * Unsupported: bitonal, rotation, does not distort aspect ratio
 * 
 * Threadsafe.
 * 
 * ImageInfo lookups are cached.
 */
public class FSIServer implements ImageServer {
    // TODO configurable..
    private static final int MAX_IMAGE_INFO_CACHE_SIZE = 1000;

    private final String baseurl;
    private final Map<String, ImageInfo> image_info_cache;
    private final ImageServerProfile profile;

    public FSIServer(String baseurl) {
        this.baseurl = baseurl;
        this.image_info_cache = new ConcurrentHashMap<String, ImageInfo>();
        this.profile = new ImageServerProfile();

        // TODO
        profile.setFormats(ImageFormat.PNG, ImageFormat.GIF, ImageFormat.JPG, ImageFormat.TIF);
        profile.setSupports();
        profile.setQualities(Quality.COLOR, Quality.GRAY);
    }

    // TODO move to switches...

    public String constructURL(ImageRequest req) throws IIIFException {
        String url = baseurl + "?type=image";
        url += "&" + param("source", req.getImageId());

        ImageInfo info = lookupImage(req.getImageId());

        if (info == null) {
            return null;
        }

        // TODO
        if (req.getFormat() == ImageFormat.PNG) {
            url += "&" + param("profile", "png");
        } else if (req.getFormat() == ImageFormat.JPG) {
            url += "&" + param("profile", "jpeg");
        } else {
            throw new IIIFException("format unsupported", "format");
        }

        Region reg = req.getRegion();

        double left, top, right, bottom;

        if (reg.getRegionType() == RegionType.FULL) {
            left = 0.0;
            top = 0.0;
            right = 1.0;
            bottom = 1.0;
        } else if (reg.getRegionType() == RegionType.ABSOLUTE) {
            double width = (double) info.getWidth();
            double height = (double) info.getHeight();

            left = reg.getX() / width;
            top = reg.getY() / height;
            right = left + (reg.getWidth() / width);
            bottom = top + (reg.getHeight() / height);

        } else if (reg.getRegionType() == RegionType.PERCENTAGE) {
            left = reg.getPercentageX() / 100.0;
            top = reg.getPercentageY() / 100.0;
            right = left + (reg.getPercentageWidth() / 100.0);
            bottom = top + (reg.getPercentageHeight() / 100.0);
        } else {
            throw new IIIFException("region unsupported", "region");
        }

        // FSI docs say this should be left,top,right,bottom but it actually
        // needs left,top,width,height all as percentages

        url += "&" + param("rect", left + "," + top + "," + (right - left) + "," + (bottom - top));

        Size scale = req.getSize();

        int width = -1, height = -1;

        if (scale.getSizeType() == SizeType.BEST_FIT) {
            width = scale.getWidth();
            height = scale.getHeight();
        } else if (scale.getSizeType() == SizeType.EXACT) {
            width = scale.getWidth();
            height = scale.getHeight();
        } else if (scale.getSizeType() == SizeType.EXACT_HEIGHT) {
            width = -1;
            height = scale.getHeight();
        } else if (scale.getSizeType() == SizeType.EXACT_WIDTH) {
            width = scale.getWidth();
            height = -1;
        } else if (scale.getSizeType() == SizeType.FULL) {
        } else if (scale.getSizeType() == SizeType.PERCENTAGE) {
            width = (int) ((right - left) * info.getWidth() * (scale.getPercentage() / 100));
            height = (int) ((bottom - top) * info.getHeight() * (scale.getPercentage() / 100));
        } else {
            throw new IIIFException("scale unsupported", "scale");
        }

        if (width != -1) {
            url += "&" + param("width", "" + width);
        }

        if (height != -1) {
            url += "&" + param("height", "" + height);
        }

        String effects = "";

        if (req.getQuality() == Quality.DEFAULT || req.getQuality() == Quality.COLOR) {
        } else if (req.getQuality() == Quality.BITONAL) {
            // TODO This can probably be supported with the right effect
            throw new IIIFException("quality unsupported", "quality");
        } else if (req.getQuality() == Quality.GRAY) {
            effects = "desaturate(lightness),";
        } else {
            throw new IIIFException("quality unsupported", "quality");
        }

        Rotation rot = req.getRotation();

        if (rot.isMirrored()) {
            effects += "flip(horizontal)";
        }

        if (rot.getAngle() != 0.0) {
            throw new IIIFException("rotation unsupported", "rotation");
        }

        if (!effects.isEmpty()) {
            url += "&" + param("effects", effects);
        }

        return url;
    }

    private String param(String name, String value) {
        try {
            return name + "=" + URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public ImageInfo lookupImage(String image_id) throws IIIFException {
        ImageInfo info = image_info_cache.get(image_id);

        if (info != null) {
            return info;
        }

        info = new ImageInfo();
        info.setImageId(image_id);

        // Dispatch a call to FSI image info service and parse the XML result

        String url = baseurl + "?type=info&tpl=info";
        url += "&" + param("source", image_id);

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();

        try {
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            URLConnection con = new URL(url).openConnection();
            con.connect();

            if (con instanceof HttpURLConnection) {
                if (((HttpURLConnection) con).getResponseCode() == 404) {
                    return null;
                }
            }

            InputStream is = con.getInputStream();
            Document doc = docBuilder.parse(is);
            is.close();

            NodeList widths = doc.getElementsByTagName("Width");
            NodeList heights = doc.getElementsByTagName("Height");

            if (widths.getLength() > 0) {
                String s = widths.item(0).getAttributes().getNamedItem("value").getTextContent();
                info.setWidth(Integer.parseInt(s));
            }

            if (heights.getLength() > 0) {
                String s = heights.item(0).getAttributes().getNamedItem("value").getTextContent();
                info.setHeight(Integer.parseInt(s));
            }
        } catch (ParserConfigurationException e) {
            throw new IIIFException(e);
        } catch (SAXException e) {
            throw new IIIFException(e);
        } catch (NumberFormatException e) {
            throw new IIIFException(e);
        } catch (IOException e) {
            throw new IIIFException(e);
        }

        if (image_info_cache.size() > MAX_IMAGE_INFO_CACHE_SIZE) {
            image_info_cache.clear();
        }

        image_info_cache.put(image_id, info);

        return info;
    }

    @Override
    public ImageServerProfile getProfile() {
        return profile;
    }

    @Override
    public ComplianceLevel getCompliance() {
        return ComplianceLevel.LEVEL_1;
    }
}
