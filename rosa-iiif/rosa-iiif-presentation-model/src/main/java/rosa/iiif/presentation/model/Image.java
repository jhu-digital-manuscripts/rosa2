package rosa.iiif.presentation.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Intended for images that are not considered annotations, but instead
 * directly embedded in a IIIF object.
 *
 * The main example of this is thumbnail images.
 *
 * For thumbnails, 'type' and 'format' are generally not required. It is
 * encouraged to have a IIIF image service available for images, but it
 * is not required.
 *
 * A thumbnail image is meant to represent a larger image. The 'depicts'
 * field allows this Image to reference the full object URI.
 */
public class Image implements Serializable {
    private static final long serialVersionUID = 1L;

    private String uri;
    private String type;
    private String format;
    private Service service;
    private String depicts;

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

    public String getDepicts() {
        return depicts;
    }

    public void setDepicts(String depicts) {
        this.depicts = depicts;
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
        if (o == null || getClass() != o.getClass()) return false;
        Image image = (Image) o;
        return width == image.width &&
                height == image.height &&
                Objects.equals(uri, image.uri) &&
                Objects.equals(type, image.type) &&
                Objects.equals(format, image.format) &&
                Objects.equals(service, image.service) &&
                Objects.equals(depicts, image.depicts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri, type, format, service, depicts, width, height);
    }

    @Override
    public String toString() {
        return "Image{" +
                "uri='" + uri + '\'' +
                ", type='" + type + '\'' +
                ", format='" + format + '\'' +
                ", service=" + service +
                ", depicts='" + depicts + '\'' +
                ", width=" + width +
                ", height=" + height +
                '}';
    }
}
