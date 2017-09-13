package rosa.iiif.presentation.model;

import java.io.Serializable;

/**
 * Intended for images that are not considered annotations, but instead
 * directly embedded in a IIIF object.
 *
 * The main example of this is thumbnail images.
 *
 * For thumbnails, 'type' and 'format' are generally not required. It is
 * encouraged to have a IIIF image service available for images, but it
 * is not required.
 */
public class Image implements Serializable {
    private static final long serialVersionUID = 1L;

    private String uri;
    private String type;
    private String format;
    private Service service;

    private int width;
    private int height;

    public Image(String uri) {
        this(uri, null, null, null);
    }

    public Image(String uri, Service service) {
        this(uri, null, null, service);
    }

    public Image(String uri, String type, String format, Service service) {
        this.uri = uri;
        this.type = type;
        this.format = format;
        this.service = service;
        width = -1;
        height = -1;
    }

    /**
     * @return does this object contain no other information other than the object URI?
     */
    public boolean onlyUri() {
        return uri != null && type != null && format != null && service != null &&
                width != -1 && height != -1;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Image)) return false;

        Image image = (Image) o;

        if (width != image.width) return false;
        if (height != image.height) return false;
        if (uri != null ? !uri.equals(image.uri) : image.uri != null) return false;
        if (type != null ? !type.equals(image.type) : image.type != null) return false;
        if (format != null ? !format.equals(image.format) : image.format != null) return false;
        return service != null ? service.equals(image.service) : image.service == null;
    }

    @Override
    public int hashCode() {
        int result = uri != null ? uri.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (format != null ? format.hashCode() : 0);
        result = 31 * result + (service != null ? service.hashCode() : 0);
        result = 31 * result + width;
        result = 31 * result + height;
        return result;
    }

    @Override
    public String toString() {
        return "Image{" +
                "uri='" + uri + '\'' +
                ", type='" + type + '\'' +
                ", format='" + format + '\'' +
                ", service=" + service +
                ", width=" + width +
                ", height=" + height +
                '}';
    }
}
