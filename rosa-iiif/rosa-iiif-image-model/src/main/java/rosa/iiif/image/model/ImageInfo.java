package rosa.iiif.image.model;

import java.io.Serializable;
import java.util.Arrays;

public class ImageInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String image_id;
    private String image_uri;
    private int width;
    private int height;
    private ImageFormat[] formats;
    private Quality[] qualities;
    private int[] sizes;
    private ServiceReference[] services;
    private ComplianceLevel compliance;
    private TileInfo[] tiles;
    private ImageServerProfile[] profiles;
    private Rights rights;

    /**
     * Create a new empty ImageInfo.
     */
    public ImageInfo() {
    }

    public String getImageId() {
        return image_id;
    }

    public void setImageId(String id) {
        this.image_id = id;
    }

    public String getImageUri() {
        return image_uri;
    }

    public void setImageUri(String uri) {
        this.image_uri = uri;
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

    public Rights getRights() {
        return rights;
    }

    public void setRights(Rights rights) {
        this.rights = rights;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImageInfo)) return false;

        ImageInfo imageInfo = (ImageInfo) o;

        if (width != imageInfo.width) return false;
        if (height != imageInfo.height) return false;
        if (image_id != null ? !image_id.equals(imageInfo.image_id) : imageInfo.image_id != null) return false;
        if (image_uri != null ? !image_uri.equals(imageInfo.image_uri) : imageInfo.image_uri != null) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(formats, imageInfo.formats)) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(qualities, imageInfo.qualities)) return false;
        if (!Arrays.equals(sizes, imageInfo.sizes)) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(services, imageInfo.services)) return false;
        if (compliance != imageInfo.compliance) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(tiles, imageInfo.tiles)) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(profiles, imageInfo.profiles)) return false;
        return !(rights != null ? !rights.equals(imageInfo.rights) : imageInfo.rights != null);

    }

    @Override
    public int hashCode() {
        int result = image_id != null ? image_id.hashCode() : 0;
        result = 31 * result + (image_uri != null ? image_uri.hashCode() : 0);
        result = 31 * result + width;
        result = 31 * result + height;
        result = 31 * result + (formats != null ? Arrays.hashCode(formats) : 0);
        result = 31 * result + (qualities != null ? Arrays.hashCode(qualities) : 0);
        result = 31 * result + (sizes != null ? Arrays.hashCode(sizes) : 0);
        result = 31 * result + (services != null ? Arrays.hashCode(services) : 0);
        result = 31 * result + (compliance != null ? compliance.hashCode() : 0);
        result = 31 * result + (tiles != null ? Arrays.hashCode(tiles) : 0);
        result = 31 * result + (profiles != null ? Arrays.hashCode(profiles) : 0);
        result = 31 * result + (rights != null ? rights.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ImageInfo{" + "image_id='" + image_id + "', image_uri='" + image_uri +
                "', width=" + width + ", height=" + height + ", formats=" + Arrays.toString(formats) +
                ", qualities=" + Arrays.toString(qualities) + ", sizes=" + Arrays.toString(sizes) +
                ", services=" + Arrays.toString(services) + ", compliance=" + compliance +
                ", tiles=" + Arrays.toString(tiles) + ", profiles=" + Arrays.toString(profiles) +
                ", rights=" + rights + '}';
    }
}
