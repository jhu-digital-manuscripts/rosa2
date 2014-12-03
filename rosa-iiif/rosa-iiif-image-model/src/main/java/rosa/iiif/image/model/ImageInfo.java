package rosa.iiif.image.model;

import java.io.Serializable;
import java.util.Arrays;

public class ImageInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String image_id;
    private String image_url;
    private int width;
    private int height;
    private ImageFormat[] formats;
    private Quality[] qualities;
    private int[] sizes;
    private ServiceReference[] services;
    private ComplianceLevel compliance;
    private TileInfo[] tiles;
    private ImageServerProfile[] profiles;

    public ImageInfo() {
    }

    public String getImageId() {
        return image_id;
    }

    public void setImageId(String id) {
        this.image_id = id;
    }

    public String getImageUrl() {
        return image_url;
    }

    public void setImageUrl(String url) {
        this.image_url = url;
    }
    
    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public ImageFormat[] getFormats() {
        return formats;
    }

    public void setFormats(ImageFormat... formats) {
        this.formats = formats;
    }

    public Quality[] getQualities() {
        return qualities;
    }

    public void setQualities(Quality... qualities) {
        this.qualities = qualities;
    }

    /**
     * @return width,height pairs
     */
    public int[] getSizes() {
        return sizes;
    }

    /**
     * @param sizes
     *            must be width,height pairs
     */
    public void setSizes(int... sizes) {
        this.sizes = sizes;
    }

    public ServiceReference[] getServices() {
        return services;
    }

    public void setServices(ServiceReference... services) {
        this.services = services;
    }

    public ComplianceLevel getCompliance() {
        return compliance;
    }

    public void setCompliance(ComplianceLevel compliance) {
        this.compliance = compliance;
    }

    public TileInfo[] getTiles() {
        return tiles;
    }

    public void setTiles(TileInfo... tiles) {
        this.tiles = tiles;
    }

    public ImageServerProfile[] getProfiles() {
        return profiles;
    }

    public void setProfiles(ImageServerProfile... profiles) {
        this.profiles = profiles;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((compliance == null) ? 0 : compliance.hashCode());
        result = prime * result + Arrays.hashCode(formats);
        result = prime * result + height;
        result = prime * result + ((image_id == null) ? 0 : image_id.hashCode());
        result = prime * result + ((image_url == null) ? 0 : image_url.hashCode());
        result = prime * result + Arrays.hashCode(profiles);
        result = prime * result + Arrays.hashCode(qualities);
        result = prime * result + Arrays.hashCode(services);
        result = prime * result + Arrays.hashCode(sizes);
        result = prime * result + Arrays.hashCode(tiles);
        result = prime * result + width;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof ImageInfo))
            return false;
        ImageInfo other = (ImageInfo) obj;
        if (compliance != other.compliance)
            return false;
        if (!Arrays.equals(formats, other.formats))
            return false;
        if (height != other.height)
            return false;
        if (image_id == null) {
            if (other.image_id != null)
                return false;
        } else if (!image_id.equals(other.image_id))
            return false;
        if (image_url == null) {
            if (other.image_url != null)
                return false;
        } else if (!image_url.equals(other.image_url))
            return false;
        if (!Arrays.equals(profiles, other.profiles))
            return false;
        if (!Arrays.equals(qualities, other.qualities))
            return false;
        if (!Arrays.equals(services, other.services))
            return false;
        if (!Arrays.equals(sizes, other.sizes))
            return false;
        if (!Arrays.equals(tiles, other.tiles))
            return false;
        if (width != other.width)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ImageInfo [image_id=" + image_id + ", image_url=" + image_url + ", width=" + width + ", height="
                + height + ", formats=" + Arrays.toString(formats) + ", qualities=" + Arrays.toString(qualities)
                + ", sizes=" + Arrays.toString(sizes) + ", services=" + Arrays.toString(services) + ", compliance="
                + compliance + ", tiles=" + Arrays.toString(tiles) + ", profiles=" + Arrays.toString(profiles) + "]";
    }
}
