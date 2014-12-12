package rosa.iiif.image.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
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
import rosa.iiif.image.model.ImageServerSupports;
import rosa.iiif.image.model.Quality;
import rosa.iiif.image.model.Region;
import rosa.iiif.image.model.RegionType;
import rosa.iiif.image.model.Rotation;
import rosa.iiif.image.model.Size;
import rosa.iiif.image.model.SizeType;

// TODO how to handle fsi result dimension limit

/**
 * Use FSI server HTTP API to fulfill IIIF requests. Image info lookups are
 * cached for performance.
 */
public class FSIServer implements ImageServer {
    private final String baseurl;
    private final ConcurrentHashMap<String, ImageInfo> image_info_cache;
    private final ImageServerProfile profile;
    private final int image_info_cache_size;

    /**
     * The base url must not end in '/' and should look something like
     * 'http://fsiserver.library.jhu.edu/server'.
     * 
     * @param baseurl
     * @param image_info_cache_size
     *            number of image info lookup responses to cache
     */
    public FSIServer(String baseurl, int image_info_cache_size) {
        this.baseurl = baseurl;
        this.image_info_cache = new ConcurrentHashMap<String, ImageInfo>(image_info_cache_size);
        this.profile = new ImageServerProfile();
        this.image_info_cache_size = image_info_cache_size;

        profile.setFormats(ImageFormat.PNG);
        profile.setSupports(ImageServerSupports.REGION_BY_PCT, ImageServerSupports.SIZE_BY_FORCED_WH,
                ImageServerSupports.SIZE_BY_WH, ImageServerSupports.PROFILE_LINK_HEADER,
                ImageServerSupports.JSONLD_MEDIA_TYPEType);
        profile.setQualities(Quality.COLOR, Quality.GRAY);
    }

    public String constructURL(ImageRequest req) throws IIIFException {
        String url = baseurl + "?type=image&" + param("source", req.getImageId());

        ImageInfo info = lookupImage(req.getImageId());

        if (info == null) {
            return null;
        }

        if (req.getFormat() == ImageFormat.PNG) {
            url += "&" + param("profile", "png");
        } else if (req.getFormat() == ImageFormat.JPG) {
            url += "&" + param("profile", "jpeg");
        } else {
            throw new IIIFException("Format unsupported: " + req.getFormat().getFileExtension(),
                    HttpURLConnection.HTTP_NOT_IMPLEMENTED);
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
            throw new IIIFException("Region unsupported: " + reg.getRegionType(),
                    HttpURLConnection.HTTP_NOT_IMPLEMENTED);
        }

        // FSI docs say this should be left,top,right,bottom but it actually
        // needs left,top,width,height all as percentages

        url += "&" + param("rect", left + "," + top + "," + (right - left) + "," + (bottom - top));

        Size scale = req.getSize();

        int width = -1, height = -1;

        if (scale.getSizeType() == SizeType.BEST_FIT) {
            if (info.getWidth() > info.getHeight()) {
                width = scale.getWidth();
                height = (scale.getHeight() * info.getHeight()) / info.getWidth();

                if (height > scale.getHeight()) {
                    int diff = height - scale.getHeight();

                    height = scale.getHeight();
                    width -= (info.getWidth() * diff / info.getHeight());
                }
            } else {
                height = scale.getHeight();
                width = (scale.getHeight() * info.getWidth()) / info.getHeight();

                if (width > scale.getWidth()) {
                    int diff = width - scale.getWidth();

                    width = scale.getWidth();
                    height -= (info.getHeight() * diff / info.getWidth());
                }
            }
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
            width = info.getWidth();
            height = info.getHeight();
        } else if (scale.getSizeType() == SizeType.PERCENTAGE) {
            width = (int) ((right - left) * info.getWidth() * (scale.getPercentage() / 100));
            height = (int) ((bottom - top) * info.getHeight() * (scale.getPercentage() / 100));
        } else {
            throw new IIIFException("Scale unsupported: " + scale.getSizeType(), HttpURLConnection.HTTP_NOT_IMPLEMENTED);
        }

        if (width != -1) {
            url += "&" + param("width", "" + width);
        }

        if (height != -1) {
            url += "&" + param("height", "" + height);
        }

        String effects = "";

        if (req.getQuality() == Quality.DEFAULT || req.getQuality() == Quality.COLOR) {
        } else if (req.getQuality() == Quality.GRAY) {
            effects = "desaturate(lightness),";
        } else {
            throw new IIIFException("Quality unsupported: " + req.getQuality().getKeyword(),
                    HttpURLConnection.HTTP_NOT_IMPLEMENTED);
        }

        Rotation rot = req.getRotation();

        if (rot.isMirrored()) {
            effects += "flip(horizontal)";
        }

        if (rot.getAngle() != 0.0) {
            throw new IIIFException("Rotation angle unsupported: " + rot.getAngle(),
                    HttpURLConnection.HTTP_NOT_IMPLEMENTED);
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

    /**
     * Extract information from FSI XML. Set image width and height.
     * 
     * @param is
     * @param info
     * @throws IIIFException
     */
    protected ImageInfo parse_image_info(InputStream is) throws IIIFException {
        ImageInfo result = new ImageInfo();

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();

        try {
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.parse(is);

            NodeList widths = doc.getElementsByTagName("Width");
            NodeList heights = doc.getElementsByTagName("Height");

            if (widths.getLength() > 0) {
                String s = widths.item(0).getAttributes().getNamedItem("value").getTextContent();
                result.setWidth(Integer.parseInt(s));
            }

            if (heights.getLength() > 0) {
                String s = heights.item(0).getAttributes().getNamedItem("value").getTextContent();
                result.setHeight(Integer.parseInt(s));
            }

            return result;
        } catch (ParserConfigurationException e) {
            throw new IIIFException(e, HttpURLConnection.HTTP_INTERNAL_ERROR);
        } catch (SAXException e) {
            throw new IIIFException(e, HttpURLConnection.HTTP_INTERNAL_ERROR);
        } catch (NumberFormatException e) {
            throw new IIIFException(e, HttpURLConnection.HTTP_INTERNAL_ERROR);
        } catch (IOException e) {
            throw new IIIFException(e, HttpURLConnection.HTTP_INTERNAL_ERROR);
        }
    }

    public ImageInfo lookupImage(String image_id) throws IIIFException {
        ImageInfo info = image_info_cache.get(image_id);

        if (info != null) {
            return info;
        }

        // Retrieve info from FSI

        String fsi_info_url = baseurl + "?type=info&tpl=info" + "&" + param("source", image_id);

        try {
            URLConnection con = new URL(fsi_info_url).openConnection();
            con.connect();

            if (con instanceof HttpURLConnection) {
                if (((HttpURLConnection) con).getResponseCode() == 404) {
                    return null;
                }
            }

            try (InputStream is = con.getInputStream()) {
                info = parse_image_info(is);

                info.setImageId(image_id);
                info.setCompliance(getCompliance());
                info.setProfiles(profile);
            }
        } catch (IOException e) {
            throw new IIIFException(e, HttpURLConnection.HTTP_INTERNAL_ERROR);
        }

        if (image_info_cache.size() > image_info_cache_size) {
            image_info_cache.clear();
        }

        image_info_cache.putIfAbsent(image_id, info);

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
